package com.example.solarShop.data.room.tables.orderAll.orderPhoto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity

@Entity(
    tableName = "order_photo_ref",
    indices = [
        Index(value = ["order_id"]),
        Index(value = ["created_at"]),
        Index(value = ["is_pinned"]),
        Index(value = ["order_id", "content_uri"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class, // نام Entity سفارش شما
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrderPhotoRefEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "order_id") val orderId: Int,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    @ColumnInfo(name = "source_type") val sourceType: Int, // 0=GALLERY, 1=CAMERA
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    @ColumnInfo(name = "local_copy_path") val localCopyPath: String? = null,
    @ColumnInfo(name = "width_px") val widthPx: Int? = null,
    @ColumnInfo(name = "height_px") val heightPx: Int? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long? = null
)

object PhotoSourceType {
    const val GALLERY = 0
    const val CAMERA = 1
}



/** جدول: هر سفارش حداکثر یک ردیف متا (کاور) دارد */
@Entity(
    tableName = "order_photo_meta",
    primaryKeys = ["order_id"],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,              // جدول orders
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OrderPhotoRefEntity::class,      // جدول order_photo_ref
            parentColumns = ["id"],
            childColumns = ["cover_photo_ref_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["order_id"]),                  // برای جوین سریع
        Index(value = ["cover_photo_ref_id"])         // برای جوین به عکس
    ]
)
data class OrderPhotoMetaEntity(
    @ColumnInfo(name = "order_id") val orderId: Int,                 // PK
    @ColumnInfo(name = "cover_photo_ref_id") val coverPhotoRefId: Int? // nullable
)

