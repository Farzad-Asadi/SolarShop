package com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OrderAnswerSelectedPhotoRepImp @Inject constructor(
    private val answerSelectedPhotoDao: OrderAnswerSelectedPhotoDao,
) : OrderAnswerSelectedPhotoRepository {
    override suspend fun upsert(entity: OrderAnswerSelectedPhotoEntity) =
        answerSelectedPhotoDao.upsert(entity)

    override fun selectedPhotoIdFlow(orderId: Int, answerId: Int): Flow<Int?> =
        answerSelectedPhotoDao.selectedPhotoIdFlow(orderId, answerId)

    override suspend fun clearSelection(orderId: Int, answerId: Int) =
        answerSelectedPhotoDao.clearSelection(orderId, answerId)

    override fun observeSelectedPhotoMap(orderId: Int): Flow<List<OrderAnswerSelectedPhotoEntity>> =
        answerSelectedPhotoDao.observeAllForOrder(orderId)

}


