package com.example.solarShop.data.backup.core

data class BackupManifest(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val modules: List<String>
)