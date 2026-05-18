package com.example.solarShop.data.room.tables.question_answers

import androidx.room.Transaction
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerDao
import com.example.solarShop.data.room.tables.question_answers.question.AnswerNextQuestionCrossRef
import com.example.solarShop.data.room.tables.question_answers.question.AnswerWithNextQuestions
import com.example.solarShop.data.room.tables.question_answers.question.QuestionDao
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity
import com.example.solarShop.data.room.tables.question_answers.question.QuestionRepository
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceDao
import com.example.solarShop.data.room.tables.selectedChoice.SelectedChoiceEntity
import com.example.solarShop.ui.questionTreeScreen.UndoAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

open class QuestionAnswersRepository @Inject constructor(
    private val questionDao: QuestionDao,    // deleteQuestionById / get / ...
    private val answerDao: AnswerDao,    // deleteQuestionById / get / ...
    private val questionRepo: QuestionRepository,
    private val selectedChoiceDao: SelectedChoiceDao,
) {
    @Transaction
    open suspend fun smartDeleteQuestion(question: QuestionEntity): SmartDeleteResult {
        val qId = question.id ?: return SmartDeleteResult.NoOp

        // 1) کودکان یکتا (سؤال‌هایی که پاسخ‌های این سؤال به آن‌ها وصل‌اند)
        val children = questionRepo.getDistinctChildrenOfQuestion(qId)

        return when {
            children.isEmpty() -> {
                // سناریو 1: هیچ خروجی ندارد → مستقیم حذف
                questionDao.deleteQuestion(question)
                SmartDeleteResult.DeletedDirectly
            }

            children.size == 1 -> {
                // سناریو 2: تمام خروجی‌ها به یک سؤال یکتا ختم می‌شوند
                val onlyChildId = children.first()

                // والدها (پاسخ‌هایی که به این سؤال اشاره کرده‌اند)
                val parents = questionRepo.getParentAnswersOfQuestion(qId)

                // یال‌های ورودی به این سؤال را حذف کن
                questionRepo.deleteAllIncomingEdgesToQuestion(qId)

                // والدها را مستقیم به تنها فرزند وصل کن (rewire)
                parents.forEach { p ->
                    val pid = p.id ?: return@forEach
                    questionRepo.insertEdge(AnswerNextQuestionCrossRef(answerId = pid, nextQuestionId = onlyChildId))
                }

                // حالا خود سؤال را حذف کن (پاسخ‌ها و یال‌های خروجی‌شان با cascade پاک می‌شوند)
                questionDao.deleteQuestion(question)
                SmartDeleteResult.RewiredAndDeleted(onlyChildId = onlyChildId, parentAnswers = parents.mapNotNull { it.id })
            }

            else -> {
                // سناریو 3: بیش از یک کودک → نیاز به تصمیم کاربر
                SmartDeleteResult.BlockedMultipleChildren(childIds = children)
            }
        }
    }

    @Transaction
    open suspend fun rewireIncomingAnswerTo(questionId: Int, newAnswerId: Int) {
        // 1) پیدا کردن اتصال والد فعلی (اگر وجود داشته باشد)
        val oldParent = questionDao.getSingleIncomingAnswer(questionId)

        // 2) اگر همان پاسخ است، کاری لازم نیست
        if (oldParent?.id == newAnswerId) return

        // 3) اگر والد قبلی بود، یالش را حذف کن
//        if (oldParent?.id != null) {
//            questionDao.deleteEdge(oldParent.id!!, questionId)
//        }

        // 4) یال جدید را بساز
        questionDao.insertEdge(AnswerNextQuestionCrossRef(
            answerId = newAnswerId,
            nextQuestionId = questionId
        ))
    }

    suspend fun edgeExists(answerId: Int, nextQuestionId: Int): Boolean =
        answerDao.countEdge(answerId, nextQuestionId) > 0

    /** آیا از fromQuestion می‌توان به toQuestion رسید؟ */
    suspend fun hasPath(fromQuestionId: Int, toQuestionId: Int): Boolean {
        if (fromQuestionId == toQuestionId) return true
        val seen = hashSetOf<Int>()
        val q = ArrayDeque<Int>()
        q.add(fromQuestionId)
        seen.add(fromQuestionId)

        while (q.isNotEmpty()) {
            val u = q.removeFirst()
            val children = answerDao.getDirectChildrenIds(u)
            for (v in children) {
                if (v == toQuestionId) return true
                if (seen.add(v)) q.addLast(v)
            }
        }
        return false
    }

    suspend fun getOwnerQuestionIdOfAnswer(answerId: Int): Int? =
        answerDao.getQuestionIdForAnswer(answerId)

    suspend fun countParentsOf(questionId: Int): Int =
        answerDao.countParents(questionId)

    suspend fun deleteEdge(answerId: Int, childQuestionId: Int): Boolean =
        questionDao.deleteEdge(answerId, childQuestionId) > 0

    fun observeAnswersWithChildrenForQuestion(questionId: Int)
            : Flow<List<AnswerWithNextQuestions>> =
        answerDao.observeAnswersWithNextQuestions(questionId)

    fun observeAnswersWithChildrenByIds(ids: List<Int>)
            : Flow<List<AnswerWithNextQuestions>> =
        if (ids.isEmpty()) flowOf(emptyList())
        else answerDao.observeAnswersWithNextQuestionsByIds(ids)


    @Transaction
    open suspend fun replaceSelectionAndPruneTailAtQuestion(
        orderId: Int,
        questionId: Int,
        answerId: Int
    ) {
        // 1) همین سؤال: فقط یک رکورد نگه می‌داریم (بدون نیاز به unique index)
        selectedChoiceDao.deleteSelectionByQuestion(orderId, questionId)
        selectedChoiceDao.insert(
            SelectedChoiceEntity(
                id = null,
                orderId = orderId,
                questionId = questionId,
                answerId = answerId
            )
        )
        // 2) همهٔ انتخاب‌های بعد از این سؤال را پاک کن
        selectedChoiceDao.deleteSelectionsInDescendants(orderId, questionId)
    }
    @Transaction
    open suspend fun replaceSelectionAtQuestionWithoutPruning(
        orderId: Int,
        questionId: Int,
        answerId: Int
    ) {
        // 1) همین سؤال: فقط یک رکورد نگه می‌داریم (بدون نیاز به unique index)
        selectedChoiceDao.deleteSelectionByQuestion(orderId, questionId)
        selectedChoiceDao.insert(
            SelectedChoiceEntity(
                id = null,
                orderId = orderId,
                questionId = questionId,
                answerId = answerId
            )
        )
    }

    suspend fun deleteSelection(orderId: Int, questionId: Int, answerId: Int) {
        // اگر «دیس‌سلکت» شد، رکورد همین پاسخ را پاک کن
        // (می‌تونی برای سادگی deleteSelectionByQuestion هم بزنی)
        selectedChoiceDao.deleteSelectionByQuestion(orderId, questionId)
        // و دُم را هم پاک کن تا مسیر ناسازگار نماند
        selectedChoiceDao.deleteSelectionsInDescendants(orderId, questionId)
    }


    suspend fun replaceParentAnswer(questionId: Int, newAnswerId: Int) =
        questionDao.replaceParentAnswer(questionId, newAnswerId)


    @Transaction
    suspend fun replaceIncomingParent(questionId: Int, newAnswerId: Int) {
        // 1) پاک کردن تمام والدهای فعلی این سوال
        questionDao.deleteIncomingEdgesOfQuestion(questionId)

        // 2) ساخت والد جدید
        questionDao.insertEdge(
            AnswerNextQuestionCrossRef(
                answerId = newAnswerId,
                nextQuestionId = questionId
            )
        )
    }


    suspend fun deleteIncomingEdgesOfQuestion(questionId: Int) =
        questionDao.deleteIncomingEdgesOfQuestion(questionId)

}

// نتیجه‌ی عملیات پاک کردن یونیت سوال برای اطلاع UI
sealed class SmartDeleteResult {
    data object NoOp : SmartDeleteResult()
    data object DeletedDirectly : SmartDeleteResult()
    data class RewiredAndDeleted(val onlyChildId: Int, val parentAnswers: List<Int>) : SmartDeleteResult()
    data class BlockedMultipleChildren(val childIds: List<Int>) : SmartDeleteResult()
}


// نتیجه‌ی عملیات ایجاد یونیت سوال برای اطلاع UI
sealed class AddChildResult {
    data class BlockedAlreadyHasChild(val answerId: Int, val existingChildIds: List<Int>) : AddChildResult()
    data class Created(val answerId: Int, val childQuestionId: Int) : AddChildResult()
}


// نتیجه‌ی عملیات اتصال به سوال موجود برای اطلاع UI
sealed class JoinChildResult {
    data class Joined(
        val answerId: Int,
        val questionId: Int,
        val undo: UndoAction
    ) : JoinChildResult()

    data class InvalidSelfParent(val answerId: Int, val questionId: Int) : JoinChildResult()
    data class AlreadyConnected(val answerId: Int, val questionId: Int) : JoinChildResult()
    data class WouldCreateCycle(val answerId: Int, val questionId: Int) : JoinChildResult()
    data class AnswerAlreadyHasChild(val answerId: Int, val existingChildIds: List<Int>) : JoinChildResult()
}



// نتیجه‌ی عملیات پاک کردن رابطه پاسخ سوال برای اطلاع UI
sealed class DeleteLinkResult {
    data class Deleted(val answerId: Int, val childQuestionId: Int) : DeleteLinkResult()
    data class NotFound(val answerId: Int) : DeleteLinkResult()                             // این Answer هیچ فرزندی ندارد
    data class NeedSelection(val answerId: Int, val childIds: List<Int>) : DeleteLinkResult() // چند فرزند → انتخاب لازم
}

// نتیجه‌ی عملیات ساخت سوال برای ریشه برای اطلاع UI
sealed class PromoteRootResult {
    data class Success(
        val newRootQuestionId: Int,
        val bridgeAnswerId: Int,
        val oldRootQuestionId: Int
    ) : PromoteRootResult()

    data class AlreadyHasParent(
        val questionId: Int,
        val parentAnswerId: Int
    ) : PromoteRootResult()

    data class Error(val message: String) : PromoteRootResult()
}

