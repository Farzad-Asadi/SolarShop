package com.example.solarShop

import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solarShop.ui.bamboApp.BamboApp
import com.example.solarShop.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appLanguageViewModel: AppLanguageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val appLanguage by appLanguageViewModel.appLanguage
                .collectAsStateWithLifecycle()

            val layoutDirection =
                if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr


            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
            ) {
                AppTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        BamboApp()
                    }
                }
            }
        }
    }




    //این ها برای پیدا کردن علت کرش در هنگام پیک آپ عکس
    override fun onSaveInstanceState(outState: Bundle) {
        logBundleSize("onSaveInstanceState(outState)", outState)
        super.onSaveInstanceState(outState)
    }
    override fun onStop() {
        // در بعضی دستگاه‌ها همینجا نزدیک‌ترین نقطه به کرشه
        Log.d("TTLE", "onStop() intentExtrasKeys=${intent.extras?.keySet()} clipData=${intent.clipData != null}")
        super.onStop()
    }




}








//این ها برای پیدا کردن علت کرش در هنگام پیک آپ عکس
private fun Bundle.approxSizeBytes(): Int {
    val p = Parcel.obtain()
    return try {
        p.writeBundle(this)
        p.dataSize()
    } finally {
        p.recycle()
    }
}
private fun logBundleSize(tag: String, b: Bundle?) {
    if (b == null) {
        Log.d("TTLE", "$tag bundle=null")
        return
    }
    Log.d("TTLE", "$tag bundleSize=${b.approxSizeBytes()} keys=${b.keySet()}")
}
