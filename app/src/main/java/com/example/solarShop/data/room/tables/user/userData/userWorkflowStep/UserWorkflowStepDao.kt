package com.example.solarShop.data.room.tables.user.userData.userWorkflowStep

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWorkflowStepDao {

    @Query("""
        SELECT * FROM user_workflow_steps
        WHERE userKey = :userKey
        ORDER BY sortOrder ASC, id ASC
    """)
    fun observeUserSteps(userKey: String): Flow<List<UserWorkflowStepEntity>>

    @Query("""
        SELECT * FROM user_workflow_steps
        WHERE userKey = :userKey
        ORDER BY sortOrder ASC, id ASC
    """)
    suspend fun getUserSteps(userKey: String): List<UserWorkflowStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: UserWorkflowStepEntity): Long

    @Update
    suspend fun updateStep(step: UserWorkflowStepEntity)

    @Delete
    suspend fun deleteStep(step: UserWorkflowStepEntity)

    @Query("DELETE FROM user_workflow_steps WHERE userKey = :userKey")
    suspend fun deleteAllStepsForUser(userKey: String)

    @Query("SELECT * FROM user_workflow_steps ORDER BY sortOrder ASC, id ASC")
    fun observeAllSteps(): Flow<List<UserWorkflowStepEntity>>

    @Query("SELECT * FROM user_workflow_steps WHERE id = :id LIMIT 1")
    suspend fun getStepById(id: Int): UserWorkflowStepEntity?

    @Query("SELECT * FROM user_workflow_steps ORDER BY sortOrder ASC, id ASC")
    fun observeTemplateSteps(): Flow<List<UserWorkflowStepEntity>>


    @Upsert
    suspend fun upsertAll(items: List<UserWorkflowStepEntity>)

    @Query("DELETE FROM user_workflow_steps WHERE id = :id")
    suspend fun deleteStepById(id: Int)

    @Query("SELECT id FROM user_workflow_steps")
    suspend fun getAllIds(): List<Int>

    @Transaction
    suspend fun replaceTemplateSteps(items: List<UserWorkflowStepEntity>) {
        upsertAll(items)

    }





    @Query("DELETE FROM user_workflow_steps WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Transaction
    suspend fun applyTemplateEdits(
        upserts: List<UserWorkflowStepEntity>,
        deletedIds: List<Int>
    ) {
        if (deletedIds.isNotEmpty()) deleteByIds(deletedIds)
        upsertAll(upserts)
    }

    @Query("""
        SELECT * FROM user_workflow_steps
        WHERE userKey = :userKey
        ORDER BY sortOrder ASC, id ASC
    """)
    fun observeTemplateSteps(userKey: String? = null): Flow<List<UserWorkflowStepEntity>>


    @Query("""
    SELECT id, title, isLocked, systemKey
    FROM user_workflow_steps
    WHERE ((:userKey IS NULL AND userKey IS NULL) OR userKey = :userKey)
""")
    suspend fun getLockInfo(userKey: String?): List<LockInfo>


    @Query("""
    SELECT * FROM user_workflow_steps
    WHERE systemKey = :systemKey AND userKey IS NULL
    LIMIT 1
""")
    suspend fun getStepBySystemKey(systemKey: String): UserWorkflowStepEntity?



    @Query("SELECT id, systemKey, isLocked, title FROM user_workflow_steps WHERE id IN (:ids)")
    suspend fun getTemplateMetaByIds(ids: List<Int>): List<TemplateMeta>

    @Query("""
    SELECT * FROM user_workflow_steps
    WHERE userKey IS NULL AND systemKey = :systemKey
    ORDER BY sortOrder ASC, id ASC
    LIMIT 1
""")
    suspend fun getTemplateStepBySystemKey(systemKey: String): UserWorkflowStepEntity?




}


data class LockInfo(
    val id: Int,
    val title: String,
    val isLocked: Boolean,
    val systemKey: String?
)

data class TemplateMeta(
    val id: Int,
    val systemKey: String?,
    val isLocked: Boolean,
    val title: String
)