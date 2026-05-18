package com.example.solarShop.data.backupRestore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.solarShop.data.room.appDatabase.AppDatabase

class SeedDumper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase
) {
    suspend fun dumpSeedDb(): File = withContext(Dispatchers.IO) {
        val outDir = File(context.getExternalFilesDir(null), "seed").apply { mkdirs() }
        val outFile = File(outDir, "bambo_seed.db")

        val sqlPath = outFile.absolutePath.replace("'", "''") // escape single quotes
        val sdb: SupportSQLiteDatabase = db.openHelper.writableDatabase

        // 1) همه چیز از WAL بیاد داخل فایل اصلی
        sdb.query("PRAGMA wal_checkpoint(FULL)").close()
        // 2) یک کپی یک‌تکه بساز (بدون wal/shm)
        sdb.execSQL("VACUUM INTO '$sqlPath'")

        outFile
    }
}
