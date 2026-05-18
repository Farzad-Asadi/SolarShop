package com.example.solarShop.data.room.tables.user.userData.userWorkflowStep

import com.example.solarShop.data.room.tables.orderAll.orderWorkflowStep.OrderWorkflowStepDao
import com.example.solarShop.data.room.tables.orderAll.orderWorkflowStep.OrderWorkflowStepEntity
import com.example.solarShop.ui.orderScreen.DraftWorkflowStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class WorkflowRepository @Inject constructor(
    private val userWorkflowStepDao: UserWorkflowStepDao,
    private val orderWorkflowStepDao: OrderWorkflowStepDao,
) {

    //region User Template

    fun observeUserSteps(userKey: String): Flow<List<UserWorkflowStepEntity>> =
        userWorkflowStepDao.observeUserSteps(userKey)

    suspend fun getUserSteps(userKey: String): List<UserWorkflowStepEntity> =
        userWorkflowStepDao.getUserSteps(userKey)

    suspend fun addStep(step: UserWorkflowStepEntity): Int =
        userWorkflowStepDao.insertStep(step).toInt()

    suspend fun updateStep(step: UserWorkflowStepEntity) =
        userWorkflowStepDao.updateStep(step)

    suspend fun deleteStep(step: UserWorkflowStepEntity) =
        userWorkflowStepDao.deleteStep(step)

    suspend fun deleteAllStepsForUser(userKey: String) =
        userWorkflowStepDao.deleteAllStepsForUser(userKey)

    //endregion

    //region Order Steps



    suspend fun getOrderSteps(orderId: Int): List<OrderWorkflowStepEntity> =
        orderWorkflowStepDao.getOrderSteps(orderId)

    /**
     * تیک‌زدن/برداشتن یک مرحله برای یک سفارش.
     * اگر ردیفی برای (orderId, stepId) وجود نداشته باشد، ساخته می‌شود.
     */
    suspend fun setStepCompleted(
        orderId: Int,
        stepId: Int,
        completed: Boolean
    ) {
        val entity = OrderWorkflowStepEntity(
            orderId = orderId,
            stepId = stepId,
            completed = completed,
            completedAt = if (completed) System.currentTimeMillis() else null
        )
        orderWorkflowStepDao.upsertOrderStep(entity)
    }





    fun observeOrderProgress(
        userKey: String,      // فعلاً استفاده نمی‌کنیم
        orderId: Int
    ): Flow<Int> {
        return combine(
            observeTemplateSteps(),                    // 👈 همه‌ی Stepها
            orderWorkflowStepDao.observeOrderSteps(orderId)
        ) { steps, orderSteps ->
            val completedIds = orderSteps
                .filter { it.completed }
                .map { it.stepId }
                .toSet()

            steps
                .filter { step ->
                    val id = step.id
                    id != null && completedIds.contains(id)
                }
                .sumOf { it.weightPercent }
        }
    }


    //endregion









    // --- Template عمومی (همان steps سیدشده) ---
    fun observeTemplateSteps(): Flow<List<UserWorkflowStepEntity>> =
        userWorkflowStepDao.observeTemplateSteps()

    // --- وضعیت steps برای یک سفارش خاص ---
    fun observeOrderSteps(orderId: Int): Flow<List<OrderWorkflowStepEntity>> =
        orderWorkflowStepDao.observeOrderSteps(orderId)

    // --- progress این سفارش بر اساس steps تیک‌خورده ---
    fun observeOrderProgress(orderId: Int): Flow<Int> {
        return combine(
            observeTemplateSteps(),
            observeOrderSteps(orderId)
        ) { steps, orderSteps ->

            val selectedStepId = orderSteps.firstOrNull { it.completed }?.stepId ?: return@combine 0

            val orderedSteps = steps.sortedWith(
                compareBy<UserWorkflowStepEntity> { it.sortOrder }.thenBy { it.id ?: Int.MAX_VALUE }
            )

            var acc = 0
            for (step in orderedSteps) {
                acc += step.weightPercent
                if (step.id == selectedStepId) break
            }
            acc
        }
    }



    // این را بعداً برای تک‌انتخابی‌کردن استفاده می‌کنیم:
    suspend fun setSingleStepCompleted(orderId: Int, stepId: Int) {
        // اول همه steps این سفارش را پاک کن
        orderWorkflowStepDao.deleteAllForOrder(orderId)

        // بعد فقط آن step را completed=true ثبت کن
        val entity = OrderWorkflowStepEntity(
            orderId = orderId,
            stepId = stepId,
            completed = true,
            completedAt = System.currentTimeMillis()
        )
        orderWorkflowStepDao.upsertOrderStep(entity)
    }

    suspend fun getStepById(stepId: Int): UserWorkflowStepEntity? =
        userWorkflowStepDao.getStepById(stepId)








    suspend fun upsertTemplateStepsFromDrafts(drafts: List<DraftWorkflowStep>) {
//        val userKey = appInfoRepo.observeAppInfo().first()?.userKey  // بسته به پروژه‌ات
        val entities = drafts.map { d ->
            UserWorkflowStepEntity(
                id = d.id?.takeIf { it > 0 },          // ✅ فقط اگر موجوده
                userKey = null,
                title = d.title,
                weightPercent = d.weightPercent,
                sortOrder = d.sortOrder
            )
        }
        userWorkflowStepDao.upsertAll(entities)
    }

    suspend fun saveTemplateEdits(
        drafts: List<DraftWorkflowStep>,
        deletedIds: List<Int>,
        userKey: String?
    ) {
        val lockInfoById = userWorkflowStepDao.getLockInfo(userKey).associateBy { it.id }

        // delete فقط برای غیرقفل‌ها
        val safeDeleted = deletedIds.filter { id -> lockInfoById[id]?.isLocked != true }

        val upserts = drafts.map { d ->
            val idPos = d.id?.takeIf { it > 0 }
            val db = idPos?.let { lockInfoById[it] }

            val finalTitle = if (db?.isLocked == true) db.title else d.title.trim()
            val finalLocked = db?.isLocked ?: d.isLocked
            val finalSystemKey = db?.systemKey // ✅ اگر ردیف قبلاً سیستمی بوده حفظ میشه

            UserWorkflowStepEntity(
                id = idPos,
                userKey = userKey,
                title = finalTitle,
                weightPercent = d.weightPercent.coerceIn(0, 100),
                sortOrder = d.sortOrder,
                isLocked = finalLocked,
                systemKey = finalSystemKey
            )
        }

        userWorkflowStepDao.applyTemplateEdits(upserts = upserts, deletedIds = safeDeleted)
    }



    suspend fun getStepBySystemKey(systemKey: String): UserWorkflowStepEntity? {
        return userWorkflowStepDao.getTemplateStepBySystemKey(systemKey)
    }





}

