package dev.hrx.libregadgets

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.hrx.libregadgets.api.LibreLinkApi
import dev.hrx.libregadgets.gadgets.updateWidget
import dev.hrx.libregadgets.communication.GlucosePollService
import dev.hrx.libregadgets.gadgets.updateComplication
import dev.hrx.libregadgets.storage.GlucoseMeasurement
import dev.hrx.libregadgets.storage.GlucoseThresholds
import dev.hrx.libregadgets.storage.MeasurementEvaluation
import dev.hrx.libregadgets.storage.MeasurementTrend
import dev.hrx.libregadgets.storage.SharedStorage
import dev.hrx.libregadgets.ui.theme.LibreGadgetsTheme
import dev.hrx.libregadgets.utils.getTimestamp
import kotlinx.coroutines.launch
import java.text.DateFormat

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
        storage =  SharedStorage(this)
        if (storage.jwtToken.isNotEmpty())
            startForegroundService(Intent(this, GlucosePollService::class.java))

        setContent {
            LibreGadgetsTheme {
                // A surface container using the 'background' color from the theme
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { padding ->
                        LoginStack(modifier = Modifier.padding(padding))
                    },
                )
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

@Composable
fun LoginStack(modifier: Modifier) {
    val (email, setEmail) = remember { mutableStateOf("") }
    val (password, setPassword) = remember { mutableStateOf("") }
    val (message, setMessage) = remember { mutableStateOf("") }
    val loading = remember { mutableStateOf(false) }
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = email,
            enabled = !loading.value,
            readOnly = loading.value,
            onValueChange = setEmail,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            label = {
                Text("Email")
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = password,
            enabled = !loading.value,
            readOnly = loading.value,
            onValueChange = setPassword,
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Password,
            ),
            visualTransformation = PasswordVisualTransformation(),
            label = {
                Text("Password")
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = !loading.value,
            onClick = {
                val api = LibreLinkApi(context)
                val storage = SharedStorage(context)

                composableScope.launch {
                    setMessage("")

                    loading.value = true
                    val response = api.login(email, password)
                    loading.value = false

                    if (response != null) {
                        storage.jwtToken = response.data.authTicket.token
                        setMessage("Login success, welcome ${response.data.user.firstName} ${response.data.user.lastName}")

                        context.startService(Intent(context, GlucosePollService::class.java))

                        val connectionResponse = api.getConnection() ?: return@launch
                        val patient = connectionResponse.data.first()

                        val measurement = GlucoseMeasurement(
                            value = patient.glucoseMeasurement.value,
                            evaluation = when {
                                patient.glucoseMeasurement.isHigh -> MeasurementEvaluation.High
                                patient.glucoseMeasurement.isLow -> MeasurementEvaluation.Low
                                else -> MeasurementEvaluation.Normal
                            },
                            trend = when (patient.glucoseMeasurement.trendArrow) {
                                1 -> MeasurementTrend.FallQuick
                                2 -> MeasurementTrend.Fall
                                3 -> MeasurementTrend.Normal
                                4 -> MeasurementTrend.Rise
                                5 -> MeasurementTrend.FallQuick
                                else -> MeasurementTrend.Unknown
                            },
                            timestamp = getTimestamp(patient.glucoseMeasurement.timestamp)
                        )
                        val thresholds = GlucoseThresholds(
                            low = patient.targetLow,
                            high = patient.targetHigh,
                        )

                        storage.latestMeasurement = measurement
                        storage.glucoseThresholds = thresholds

                        updateWidget(context)
                        updateComplication(context)

                        context.startForegroundService(Intent(context, GlucosePollService::class.java))
                    } else {
                        setMessage("Login failure")
                    }
                }
            }
        ) {
            Text(text = "Login")
        }

        if (loading.value || message.isNotEmpty())
            Spacer(modifier = Modifier.height(16.dp))

        if (loading.value)
            CircularProgressIndicator()
        else if (message.isNotEmpty())
            Text(text = message)
    }
}
