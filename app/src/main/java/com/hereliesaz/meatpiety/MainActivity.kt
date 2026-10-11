package com.hereliesaz.meatpiety

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.hereliesaz.meatpiety.widget.updateAllWidgets
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Milestones crossed before permission is granted fire the next time the ledger changes.
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.init(this)
        Notifier.init(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent { MeatPietyApp() }
    }

    override fun onStop() {
        super.onStop()
        // The start date or diet may have changed; bring the home-screen widgets up to date.
        lifecycleScope.launch { updateAllWidgets(applicationContext) }
    }
}
