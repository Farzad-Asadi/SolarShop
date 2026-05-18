package com.example.solarShop.data.room.tables.orderAll.priceEstimate

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.solarShop.ui.orderScreen.orderPriceEstimate.ClosetMode

import com.example.solarShop.utils.EstimateApproach
import com.example.solarShop.utils.EstimateCategory
import kotlinx.coroutines.flow.Flow


@Dao
interface PriceEstimateDao {
    @Query(
        """
SELECT * FROM price_estimates
WHERE orderId = :orderId AND category = :category
LIMIT 1
"""
    )
    fun observe(
        orderId: Int,
        category: EstimateCategory = EstimateCategory.CABINET
    ): Flow<PriceEstimateEntity?>


    @Query(
        """
SELECT * FROM price_estimates
WHERE orderId = :orderId AND category = :category
LIMIT 1
"""
    )
    suspend fun getOnce(
        orderId: Int,
        category: EstimateCategory = EstimateCategory.CABINET
    ): PriceEstimateEntity?


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PriceEstimateEntity): Long


    @Update
    suspend fun update(entity: PriceEstimateEntity): Int


    @Query("UPDATE price_estimates SET inputsJson = :inputsJson, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateInputsJson(
        id: Int,
        inputsJson: String,
        updatedAt: Long = System.currentTimeMillis()
    )


    @Query(
        """
UPDATE price_estimates
SET approach = :approach, updatedAt = :updatedAt
WHERE id = :id
"""
    )
    suspend fun updateApproach(
        id: Int,
        approach: EstimateApproach,
        updatedAt: Long = System.currentTimeMillis()
    )


    @Query(
        """
UPDATE price_estimates
SET pricePerMeterOverride = :ppm,
platePriceOverride = :plate,
installationFeePerMeterOverride = :install,
transportationPrice = :transport,
updatedAt = :updatedAt
WHERE id = :id
"""
    )
    suspend fun updateHeaderOverrides(
        id: Int,
        ppm: Long?,
        plate: Long?,
        install: Long?,
        transport: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )


    @Query(
        """
        SELECT * FROM price_estimates 
        WHERE orderId = :orderId AND category = :category 
        LIMIT 1
    """
    )
    fun observeByOrderAndCategory(
        orderId: Int,
        category: EstimateCategory
    ): Flow<PriceEstimateEntity?>

    @Query(
        """
        SELECT * FROM price_estimates 
        WHERE orderId = :orderId AND category = :category 
        LIMIT 1
    """
    )
    suspend fun getByOrderAndCategory(
        orderId: Int,
        category: EstimateCategory
    ): PriceEstimateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PriceEstimateEntity): Long


    @Query("""
        UPDATE price_estimates
        SET priceEstimateResult = :result,
            updatedAt = :updatedAt
        WHERE id = :estimateId
    """)
    suspend fun updateEstimateResult(
        estimateId: Int,
        result: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )


    @Query("""
    SELECT * FROM price_estimates
    WHERE orderId = :orderId
    ORDER BY updatedAt DESC, id DESC
    LIMIT 1
""")
    fun observeLatestByOrderId(orderId: Int): Flow<PriceEstimateEntity?>

}

@kotlinx.serialization.Serializable
data class ClosetInputsJson(
    val kind: String = "closet",
    val mode: String = ClosetMode.SIMPLE.name,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val depth: Double = 0.60,
    val stdDepth: Double = 0.60
)
