package dev.hrx.libregadgets

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import dev.hrx.libregadgets.communication.GlucosePollService
import dev.hrx.libregadgets.storage.SharedStorage
import dev.hrx.libregadgets.ui.HomePage
import dev.hrx.libregadgets.ui.LoginPage
import dev.hrx.libregadgets.ui.theme.LibreGadgetsTheme

class MainActivity : ComponentActivity() {
    private lateinit var storage: SharedStorage
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // if permission was denied, the service can still run only the notification won't be visible
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestNotificationPermission()
        storage = SharedStorage(this)
        if (storage.jwtToken.isNotEmpty())
            startForegroundService(Intent(this, GlucosePollService::class.java))

        setContent {
            LibreGadgetsTheme {
                // A surface container using the 'background' color from the theme
                if (storage.jwtToken.isNotEmpty()) {
                    HomePage()
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        content = { padding ->

                            LoginPage(modifier = Modifier.padding(padding))
                        },
                    )
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    // permission already granted
                }

                else -> {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

