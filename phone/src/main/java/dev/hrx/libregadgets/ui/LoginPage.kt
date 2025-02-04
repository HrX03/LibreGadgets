package dev.hrx.libregadgets.ui

import android.content.Intent
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.hrx.libregadgets.core.api.LibreLinkApi
import dev.hrx.libregadgets.core.api.types.LoginResponse
import dev.hrx.libregadgets.core.communication.GlucosePollService
import dev.hrx.libregadgets.gadgets.updateComplication
import dev.hrx.libregadgets.gadgets.updateWidget
import dev.hrx.libregadgets.core.storage.GlucoseMeasurement
import dev.hrx.libregadgets.core.storage.GlucoseThresholds
import dev.hrx.libregadgets.core.storage.MeasurementEvaluation
import dev.hrx.libregadgets.core.storage.MeasurementTrend
import dev.hrx.libregadgets.core.storage.SharedStorage
import dev.hrx.libregadgets.core.utils.getTimestamp
import kotlinx.coroutines.launch

@Composable
fun LoginPage(modifier: Modifier) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
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
            onValueChange = { email = it },
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
            onValueChange = { password = it },
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
                    message = ""

                    loading.value = true
                    var response = api.login(email, password)
                    loading.value = false

                    if(response == null) {
                        message = "Could not contact API endpoint or other undefined error"
                        return@launch
                    } else if(response is LoginResponse.LoginErrorResponse) {
                        message = response.error.message
                        return@launch
                    }

                    if(response is LoginResponse.LoginRedirectResponse) {
                        return@launch
                    }

                    response = response as LoginResponse.LoginSuccessResponse
                    storage.jwtToken = response.data.authTicket.token
                    storage.accountId = response.data.user.id
                    message = "Login success, welcome ${response.data.user.firstName} ${response.data.user.lastName}"

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