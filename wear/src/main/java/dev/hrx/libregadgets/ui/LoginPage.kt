package dev.hrx.libregadgets.ui

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryDefaults.scrollBehavior
import androidx.wear.compose.foundation.rotary.rotary
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import com.google.android.horologist.compose.layout.ScreenScaffold
import dev.hrx.libregadgets.core.api.LibreLinkApi
import dev.hrx.libregadgets.core.api.types.LoginResponse
import dev.hrx.libregadgets.core.communication.GlucosePollService
import dev.hrx.libregadgets.core.storage.GlucoseMeasurement
import dev.hrx.libregadgets.core.storage.GlucoseThresholds
import dev.hrx.libregadgets.core.storage.MeasurementEvaluation
import dev.hrx.libregadgets.core.storage.MeasurementTrend
import dev.hrx.libregadgets.core.storage.SharedStorage
import dev.hrx.libregadgets.core.utils.getTimestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun LoginPage() {
    val listState = rememberScalingLazyListState()

    ScreenScaffold(
        scrollState = listState,
    ) {
        val focusRequester = rememberActiveFocusRequester()
        val coroutineScope = rememberCoroutineScope()
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        val loading = remember { mutableStateOf(false) }
        val context = LocalContext.current

        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .rotary(
                    rotaryBehavior = scrollBehavior(listState),
                    focusRequester = focusRequester,
                )
                .focusable(),
            state = listState,
        ) {
            item {
                TextInput(
                    value = email,
                    enabled = !loading.value,
                    onChange = { email = it },
                    placeholder = "Email"
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                TextInput(
                    value = password,
                    enabled = !loading.value,
                    onChange = { password = it },
                    placeholder = "Password",
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Button(
                    enabled = !loading.value,
                    onClick = {
                        val api = LibreLinkApi(context)
                        val storage = SharedStorage(context)

                        coroutineScope.launch {
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



                            context.startForegroundService(Intent(context, GlucosePollService::class.java))
                        }
                    }
                ) {
                    Text(text = "Login")
                }
            }

            item {
                if (loading.value || message.isNotEmpty())
                    Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                if (loading.value)
                    CircularProgressIndicator()
                else if (message.isNotEmpty())
                    Text(text = message)
            }
        }
    }
}

@Composable
fun TextInput(
    placeholder: String,
    value: String?,
    onChange: (value: String) -> Unit,
    enabled: Boolean,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { data ->
                val results: Bundle = RemoteInput.getResultsFromIntent(data)
                val newValue: CharSequence? = results.getCharSequence(placeholder)
                onChange(newValue as String)
            }
        }
    Column {
        Chip(
            enabled = enabled,
            label = { Text(if (value.isNullOrEmpty()) placeholder else value) },
            onClick = {
                val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent();
                val remoteInputs: List<RemoteInput> = listOf(
                    RemoteInput.Builder(placeholder)
                        .setLabel(placeholder)
                        .wearableExtender {
                            setEmojisAllowed(false)
                            setInputActionType(EditorInfo.IME_ACTION_DONE)
                        }.build()
                )

                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)

                launcher.launch(intent)
            }
        )
    }
}