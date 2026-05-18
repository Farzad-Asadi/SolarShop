package com.example.solarShop.utils

import com.example.solarShop.data.room.tables.question_answers.answer.AnswerEntity
import com.example.solarShop.data.room.tables.question_answers.question.AnswerNextQuestionCrossRef
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity


fun createDefaultQuestion(): MutableList<Pair<QuestionEntity, List<AnswerEntity>>> {

    val defaultQuestionEntityAnswerEntityList: MutableList<Pair<QuestionEntity, List<AnswerEntity>>> = mutableListOf()

    defaultQuestionEntityAnswerEntityList.add(
        Pair(
            QuestionEntity(
                id = 1,
                title = "انتخاب نوع سفارش",
            ),
            listOf(
                AnswerEntity(
                    id = 1,
                    questionId = 1,
                    title = "کابینت",
                ),
                AnswerEntity(
                    id = 2,
                    questionId = 1,
                    title = "کمد",
                )
            )
        ),
    )
    defaultQuestionEntityAnswerEntityList.add(
        Pair(
            QuestionEntity(
                id = 2,
                title = "انتخاب فلزی یا غیرفلزی",
            ),
            listOf(
                AnswerEntity(
                    id = 3,
                    questionId = 2,
                    title = "غیر فلزی",
                ),
                AnswerEntity(
                    id = 4,
                    questionId = 2,
                    title = "فلزی",
                )
            )
        ),
    )
    defaultQuestionEntityAnswerEntityList.add(
        Pair(
            QuestionEntity(
                id = 3,
                title = "انتخاب سبک",
            ),
            listOf(
                AnswerEntity(
                    id = 5,
                    questionId = 3,
                    title = "مدرن",
                ),
                AnswerEntity(
                    id = 6,
                    questionId = 3,
                    title = "کلاسیک",
                ),
                AnswerEntity(
                    id = 7,
                    questionId = 3,
                    title = "نئوکلاسیک",
                )

            )
        ),
    )
    defaultQuestionEntityAnswerEntityList.add(
        Pair(
            QuestionEntity(
                id = 4,
                title = "انواع چیدمان مدرن",
            ),
            listOf(
                AnswerEntity(
                    id = 8,
                    questionId = 4,
                    title = " موازی",
                ),
                AnswerEntity(
                    id = 9,
                    questionId = 4,
                    title = "ال",
                ),
                AnswerEntity(
                    id = 11,
                    questionId = 4,
                    title = "خطی",
                ),
                AnswerEntity(
                    id = 10,
                    questionId = 4,
                    title = " یو ",
                )

            )
        ),
    )
    defaultQuestionEntityAnswerEntityList.add(
        Pair(
            QuestionEntity(
                id = 5,
                title = "انتخاب فلزی یا غیرفلزی",
            ),
            listOf(
                AnswerEntity(
                    id = 12,
                    questionId = 5,
                    title = "غیر فلزی",
                ),
                AnswerEntity(
                    id = 13,
                    questionId = 5,
                    title = "فلزی",
                )
            )
        ),
    )

    return defaultQuestionEntityAnswerEntityList

}

fun createDefaultQuestionsAndRouting(): Pair<List<Pair<QuestionEntity, List<AnswerEntity>>>, List<AnswerNextQuestionCrossRef>> {

    val questionsAndAnswers = createDefaultQuestion()

    val routing = listOf(
        AnswerNextQuestionCrossRef(answerId = 1, nextQuestionId = 2),
        AnswerNextQuestionCrossRef(answerId = 2, nextQuestionId = 5),
        AnswerNextQuestionCrossRef(answerId = 3, nextQuestionId = 3),
        AnswerNextQuestionCrossRef(answerId = 5, nextQuestionId = 4),
    )

    return Pair(questionsAndAnswers, routing)
}

