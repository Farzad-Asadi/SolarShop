package com.example.solarShop.data.room.tables.question_answers.question

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import com.example.solarShop.data.room.tables.user.UserEntity

@Entity(
    tableName = "questions",
    indices = [Index("userKey")],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userKey"],
            childColumns = ["userKey"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val userKey:String?=null,
    val title: String,
    val isHidden: Boolean = false,
    val isRoot: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)


data class QuestionWithAnswers(
    @Embedded
    val question: QuestionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val answers: List<AnswerEntity>
)

data class AnswerWithParentQuestion(
    @Embedded
    val answer: AnswerEntity,

    @Relation(
        parentColumn = "questionId",   // در AnswerEntity
        entityColumn = "id"            // در QuestionEntity
    )
    val parentQuestion: QuestionEntity
)



@Entity(
    tableName = "answer_next_questions",
    primaryKeys = ["answerId", "nextQuestionId"],
    indices = [Index("answerId"), Index("nextQuestionId")],
    foreignKeys = [
        ForeignKey(
            entity = AnswerEntity::class,
            parentColumns = ["id"],
            childColumns = ["answerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["nextQuestionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AnswerNextQuestionCrossRef(
    val answerId: Int,
    val nextQuestionId: Int
)



data class AnswerWithNextQuestions(
    @Embedded
    val answer: AnswerEntity,
    @Relation(
        parentColumn = "id",            // answers.id
        entityColumn = "id",            // questions.id
        associateBy = Junction(
            value = AnswerNextQuestionCrossRef::class,
            parentColumn = "answerId",        // از جدول واسط: ستون مربوط به Answer
            entityColumn = "nextQuestionId"   // از جدول واسط: ستون مربوط به Question
        )
    )
    val nextQuestions: List<QuestionEntity>
)


data class ChoiceEdgeRow(val questionId: Int?, val answerId: Int? ,val choiceDescription:String?,)



