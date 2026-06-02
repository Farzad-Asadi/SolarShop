package com.example.solarShop.data.backup.core

const val CURRENT_BACKUP_VERSION = 1

data class BackupManifest(
    val version: Int = CURRENT_BACKUP_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val modules: List<String>
)