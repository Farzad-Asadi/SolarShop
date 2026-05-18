package com.example.solarShop.data.room.tables.selectedChoice.answerSelectedPhoto

import kotlinx.coroutines.flow.Flow

interface OrderAnswerSelectedPhotoRepository {

    suspend fun upsert(entity: OrderAnswerSelectedPhotoEntity)

    fun selectedPhotoIdFlow(orderId: Int, answerId: Int): Flow<Int?>

    suspend fun clearSelection(orderId: Int, answerId: Int)

    fun observeSelectedPhotoMap(orderId: Int): Flow<List<OrderAnswerSelectedPhotoEntity>>
}