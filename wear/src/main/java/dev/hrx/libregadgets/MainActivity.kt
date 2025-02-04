/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package dev.hrx.libregadgets

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.horologist.compose.layout.AppScaffold
import dev.hrx.libregadgets.core.communication.GlucosePollService
import dev.hrx.libregadgets.core.storage.SharedStorage
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
        installSplashScreen()
        checkAndRequestNotificationPermission()
        storage = SharedStorage(this)
        if (storage.jwtToken.isNotEmpty())
            startForegroundService(Intent(this, GlucosePollService::class.java))

        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            LibreGadgetsTheme {
                AppScaffold {
                    LoginPage()
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
