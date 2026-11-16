package com.leonardovictormuller.napstop.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonardovictormuller.napstop.domain.model.TrackingState
import kotlinx.coroutines.launch

// Semantic test tags for E2E testing
object HomeTestTags {
    const val SCREEN_ROOT = "home_screen_root"
    const val DESTINATION_NAME_INPUT = "destination_name_input"
    const val LATITUDE_INPUT = "latitude_input"
    const val LONGITUDE_INPUT = "longitude_input"
    const val VIBRATION_SLIDER = "vibration_slider"
    const val ALARM_SLIDER = "alarm_slider"
    const val START_BUTTON = "start_button"
    const val STOP_BUTTON = "stop_button"
    const val STATUS_TEXT = "status_text"
    const val ETA_TEXT = "eta_text"
    const val DISTANCE_TEXT = "distance_text"
    const val PRESET_PREFIX = "preset_"
}

// Quick destination presets for simulation and demos
data class DestinationPreset(val name: String, val lat: Double, val lon: Double)

val defaultPresets = listOf(
    DestinationPreset("Estação Paulista", -23.5635, -46.6549),
    DestinationPreset("MASP", -23.5614, -46.6557),
    DestinationPreset("Parque Ibirapuera", -23.5874, -46.6576),
    DestinationPreset("Estação Consolação", -23.5556, -46.6611),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Permission launcher for fine & coarse location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.onEvent(HomeEvent.StartMonitoring)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Permissão de localização é necessária para o monitoramento.")
            }
        }
    }

    fun requestLocationAndStart() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.onEvent(HomeEvent.StartMonitoring)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    // Show errors via Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(HomeEvent.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("NapStop 🛑") },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag(HomeTestTags.SCREEN_ROOT),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- Status Card ---
            StatusCard(uiState)

            // --- Destination Input ---
            Text("Destino", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = uiState.destinationNameInput,
                onValueChange = { viewModel.onEvent(HomeEvent.DestinationNameChanged(it)) },
                label = { Text("Nome do ponto") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HomeTestTags.DESTINATION_NAME_INPUT),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.latitudeInput,
                    onValueChange = { viewModel.onEvent(HomeEvent.LatitudeChanged(it)) },
                    label = { Text("Latitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(HomeTestTags.LATITUDE_INPUT),
                )
                OutlinedTextField(
                    value = uiState.longitudeInput,
                    onValueChange = { viewModel.onEvent(HomeEvent.LongitudeChanged(it)) },
                    label = { Text("Longitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(HomeTestTags.LONGITUDE_INPUT),
                )
            }

            // --- Presets ---
            Text("Presets rápidos", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                defaultPresets.forEach { preset ->
                    OutlinedButton(
                        onClick = {
                            viewModel.onEvent(
                                HomeEvent.PresetSelected(preset.name, preset.lat, preset.lon)
                            )
                        },
                        modifier = Modifier.testTag("${HomeTestTags.PRESET_PREFIX}${preset.name}"),
                    ) {
                        Text(preset.name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // --- Alert Config ---
            Text("Configuração de Alertas", style = MaterialTheme.typography.titleMedium)

            Text(
                "Vibração: ${uiState.vibrationLeadTimeMinutes} min antes",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = uiState.vibrationLeadTimeMinutes.toFloat(),
                onValueChange = { viewModel.onEvent(HomeEvent.VibrationLeadTimeChanged(it.toInt())) },
                valueRange = 2f..30f,
                steps = 27,
                modifier = Modifier.testTag(HomeTestTags.VIBRATION_SLIDER),
            )

            Text(
                "Alarme Sonoro: ${uiState.alarmLeadTimeMinutes} min antes",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = uiState.alarmLeadTimeMinutes.toFloat(),
                onValueChange = { viewModel.onEvent(HomeEvent.AlarmLeadTimeChanged(it.toInt())) },
                valueRange = 1f..20f,
                steps = 18,
                modifier = Modifier.testTag(HomeTestTags.ALARM_SLIDER),
            )

            // --- Action Buttons ---
            val isMonitoring = uiState.trackingState !is TrackingState.Idle

            if (!isMonitoring) {
                Button(
                    onClick = { requestLocationAndStart() },
                    enabled = uiState.isStartEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(HomeTestTags.START_BUTTON),
                ) {
                    Text("Iniciar Monitoramento")
                }
            } else {
                Button(
                    onClick = { viewModel.onEvent(HomeEvent.StopMonitoring) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(HomeTestTags.STOP_BUTTON),
                ) {
                    Text("Parar Monitoramento")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusCard(uiState: HomeUiState) {
    val (statusLabel, containerColor) = when (uiState.trackingState) {
        is TrackingState.Idle -> "Aguardando configuração" to MaterialTheme.colorScheme.surfaceVariant
        is TrackingState.Monitoring -> "✅ Monitorando" to MaterialTheme.colorScheme.primaryContainer
        is TrackingState.VibrationTriggered -> "📳 Vibração ativada!" to MaterialTheme.colorScheme.secondaryContainer
        is TrackingState.AlarmTriggered -> "🚨 ALARME! Prepare-se!" to MaterialTheme.colorScheme.errorContainer
        is TrackingState.Error -> "❌ Erro" to MaterialTheme.colorScheme.errorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(HomeTestTags.STATUS_TEXT),
            )

            when (val state = uiState.trackingState) {
                is TrackingState.Monitoring -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "ETA: ${"%.1f".format(state.etaMinutes)} min",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag(HomeTestTags.ETA_TEXT),
                    )
                    Text(
                        text = "Distância: ${"%.2f".format(state.distanceKm)} km",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(HomeTestTags.DISTANCE_TEXT),
                    )
                }
                is TrackingState.VibrationTriggered -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "ETA: ${"%.1f".format(state.etaMinutes)} min",
                        modifier = Modifier.testTag(HomeTestTags.ETA_TEXT),
                    )
                }
                is TrackingState.AlarmTriggered -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "ETA: ${"%.1f".format(state.etaMinutes)} min",
                        modifier = Modifier.testTag(HomeTestTags.ETA_TEXT),
                    )
                }
                is TrackingState.Error -> {
                    Spacer(Modifier.height(8.dp))
                    Text(text = state.message, style = MaterialTheme.typography.bodySmall)
                }
                is TrackingState.Idle -> Unit
            }
        }
    }
}
