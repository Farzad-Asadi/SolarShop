package com.example.solarShop.data.room.tables.question_answers.answer

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity

@Entity(
    tableName = "answers",
    indices = [Index("questionId")],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int?=null,
    val questionId:Int,
    val title:String,
    val note:String = "",
    val isLiked: Boolean = false,
    val isHidden: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)



@Entity(
    tableName = "answer_images",
    indices = [Index("answerId")],
    foreignKeys = [
        ForeignKey(
            entity = AnswerEntity::class,
            parentColumns = ["id"],
            childColumns = ["answerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AnswerImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val answerId: Int,
    // فقط نام فایل را ذخیره کن؛ مسیر کامل را با filesDir می‌سازیم
    val fileName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)





data class AnswerWithImages(
    @Embedded val answer: AnswerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "answerId",
        entity = AnswerImageEntity::class
    )
    val images: List<AnswerImageEntity>
)