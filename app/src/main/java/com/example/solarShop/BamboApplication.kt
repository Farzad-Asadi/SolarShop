package com.example.solarShop

import android.app.Application
import android.content.SharedPreferences
import com.example.solarShop.data.backupRestore.RestoreManager
import com.example.solarShop.data.backupRestore.v2.RestoreContext
import com.example.solarShop.data.backupRestore.v2.RestoreManagerV2
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BamboApplication : Application() {

    @Inject lateinit var restoreManagerV2: RestoreManagerV2
    @Inject lateinit var restoreCtx: RestoreContext
    @Inject lateinit var restoreManager: RestoreManager
    @Inject lateinit var sharedPrefs: SharedPreferences


    //    private fun overlayFromAssetsIfFirstRunV2() {
//        val isFirstRun = sharedPrefs.getBoolean("is_first_run", true)
//        if (!isFirstRun) return
//
//        runBlocking {
//            try {
//                withContext(Dispatchers.IO) {
//                    // 1) کپی از assets به temp file (RestoreManagerV2 با File کار می‌کند)
//                    val tmpZip = File(cacheDir, "seed_v2_${System.currentTimeMillis()}.zip")
//                    assets.open("seed/bambo_backupAll.zip").use { ins ->
//                        tmpZip.outputStream().use { outs -> ins.copyTo(outs) }
//                    }
//
//                    // 2) اجرای ریستور V2 (فعلاً QNA + CONTRACTS چون Providerهای این دو تا بایند شده‌اند)
//                    val opts = RestoreOptions(
//                        selected = setOf(BackupCategory.QNA, BackupCategory.CONTRACTS),
//                        conflictPolicy = ConflictPolicy.Overwrite,
//                        userKeyMapping = UserKeyMapping.RemapToCurrent
//                    )
//
//                    // اتمیک بهتر: همه‌چیز داخل یک تراکنش
//                    restoreCtx.db.withTransaction {
//                        val reports = restoreManagerV2.restoreSelected(tmpZip, opts, restoreCtx)
//                        Log.d("BamboApp", "Bootstrap V2 restore OK, reports=$reports")
//                    }
//
//                    tmpZip.delete()
//                }
//            } catch (t: Throwable) {
//                Log.e("BamboApp", "Bootstrap V2 restore FAILED", t)
//            }
//        }
//
//        sharedPrefs.edit().putBoolean("is_first_run", false).apply()
//    }




//------------------ 2.1 -----------------
//
//    @Inject lateinit var db: AppDatabase
//    @Inject lateinit var appScope: CoroutineScope
//    @Inject lateinit var contractsTemplateSeeder: ContractsTemplateSeeder
//    @Inject lateinit var invoiceTemplateSeeder: InvoiceTemplateSeeder
//    @Inject lateinit var expenseCategorySeeder: ExpenseCategorySeeder
//
//
//    // اسکوپ سطح اپلیکیشن  3.2
//    override fun onCreate() {
//        super.onCreate()
//        seedIfFirstRun()
//    }
//    private fun seedIfFirstRun() {
////        val isFirstRun = sharedPrefs.getBoolean("is_first_run", true)
////        if (!isFirstRun) return
//        val (questions, answerNextQuestionList) = createDefaultQuestionsAndRouting()
//        val workflowSteps = createDefaultWorkflowSteps()
//        appScope.launch {
//            db.withTransaction {
////                 1) اطلاعات اپ
//                db.appInfoDao().insertAppInfo(AppInfoEntity())
////                 2) سوال/جواب‌ها
//                questions.forEach { (question, choices) ->
//                    db.questionDao().insertQuestion(question)
//                    choices.forEach { choice ->
//                        db.answerDao().insertAnswer(choice)
//                    }
//                }
//                db.questionDao().upsertAnswerNextQuestions(answerNextQuestionList)
//                // 3) قالب‌های قرارداد (فقط اگر خالی است)
//                contractsTemplateSeeder.insertDefaultsIfEmpty()
//
//                // 4) تمپلیت‌های پیش‌فاکتور / فاکتور (فقط اگر خالی است)
//                invoiceTemplateSeeder.insertDefaultsIfEmpty()
//
//                // 5) مراحل پیش‌فرض workflow برای کاربر پیش‌فرض
//                workflowSteps.forEach { step ->
//                    db.userWorkflowStepDao().insertStep(step)
//                }
//
//                // ✅ دسته‌های هزینه (فقط اگر خالی است)
//                expenseCategorySeeder.insertDefaultsIfEmpty()
//
//            }
////            sharedPrefs.edit().putBoolean("is_first_run", false).apply()
//        }
//    }

















































//------------------ old ver. -----------------

//    override fun onCreate() {
//        super.onCreate()
//
//        overlayFromAssetsIfFirstRun()
//    }
//    private fun overlayFromAssetsIfFirstRun() {
//        val isFirstRun = sharedPrefs.getBoolean("is_first_run", true)
//        if (!isFirstRun) return
//        kotlinx.coroutines.runBlocking {
//            try {
//                assets.open("seed/bambo_backup.zip").use { ins ->
//                    val copied = restoreManager.restoreFromZip(ins)
//                    android.util.Log.d("BamboApp", "Bootstrap restore OK, images=$copied")
//                }
//            } catch (t: Throwable) {
//                android.util.Log.e("BamboApp", "Bootstrap restore FAILED", t)
//            }
//        }
//        sharedPrefs.edit().putBoolean("is_first_run", false).apply()
//    }



















}
