package com.example.solarShop.data.backup.core


data class BackupPreviewInfo(
    val version: Int,
    val createdAt: Long,
    val modules: List<String>,

    val categoryCount: Int = 0,
    val brandCount: Int = 0,
    val productCount: Int = 0,
    val imageCount: Int = 0
)