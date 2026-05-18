package com.example.solarShop.data.room.tables.user


import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.solarShop.USER_ROLE_CABINET_MAKER
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.question_answers.question.QuestionEntity

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["mobilePhone"], unique = true),
        Index(value = ["userKey"], unique = true) // کلید پایدار
    ]
)
data class UserEntity(
    @PrimaryKey val id : Int,
    val name: String="",
    val userKey: String ,
    val mobilePhone: String,
    val landlinePhone: String="",
    val nationalCode: String="",
    val workshop: String="",
    val address: String="",
    val avatar: String="",
    val role : String=USER_ROLE_CABINET_MAKER,
    val createdAt: Long,
    val updatedAt: Long?=null
)


data class UserWithClients(
    @Embedded
    val userEntity: UserEntity,
    @Relation(
        parentColumn = "userKey",
        entityColumn = "userKey"
    )
    val clientEntities: List<ClientEntity>
)





data class UserWithQuestions(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val questions: List<QuestionEntity>
)




















