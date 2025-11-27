package com.zaneschepke.wireguardautotunnel.ui.common.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.state.MimicSettings
import com.zaneschepke.wireguardautotunnel.ui.state.MimicType

@Composable
fun MimicConfigDialog(
    mimicType: MimicType,
    currentSettings: MimicSettings,
    onDismiss: () -> Unit,
    onApply: (MimicSettings) -> Unit,
    onGenerate: (MimicSettings) -> Unit,
) {
    var domain by remember { mutableStateOf(currentSettings.domain) }
    var sipFromUser by remember { mutableStateOf(currentSettings.sipFromUser) }
    var sipToUser by remember { mutableStateOf(currentSettings.sipToUser) }
    var sipFromDomain by remember { mutableStateOf(currentSettings.sipFromDomain) }
    var sipToDomain by remember { mutableStateOf(currentSettings.sipToDomain) }
    var quicVersion by remember { mutableStateOf(currentSettings.quicVersion) }
    var itimeMin by remember { mutableStateOf(currentSettings.itimeMin.toString()) }
    var itimeMax by remember { mutableStateOf(currentSettings.itimeMax.toString()) }
    var regenerateInterval by remember { mutableStateOf(currentSettings.regenerateIntervalSeconds.toString()) }

    val title = when (mimicType) {
        MimicType.DNS -> stringResource(R.string.mimic_dns_settings)
        MimicType.QUIC -> stringResource(R.string.mimic_quic_settings)
        MimicType.SIP -> stringResource(R.string.mimic_sip_settings)
    }

    fun buildSettings(): MimicSettings {
        return MimicSettings(
            type = mimicType,
            domain = domain,
            sipFromUser = sipFromUser,
            sipToUser = sipToUser,
            sipFromDomain = sipFromDomain,
            sipToDomain = sipToDomain,
            quicVersion = quicVersion,
            itimeMin = itimeMin.toIntOrNull()?.coerceIn(
                MimicSettings.ITIME_MIN_ALLOWED,
                MimicSettings.ITIME_MAX_ALLOWED
            ) ?: 120,
            itimeMax = itimeMax.toIntOrNull()?.coerceIn(
                MimicSettings.ITIME_MIN_ALLOWED,
                MimicSettings.ITIME_MAX_ALLOWED
            ) ?: 180,
            regenerateIntervalSeconds = regenerateInterval.toIntOrNull()?.coerceIn(
                MimicSettings.REGENERATE_MIN,
                MimicSettings.REGENERATE_MAX
            ) ?: MimicSettings.DEFAULT_REGENERATE_INTERVAL
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp),
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (mimicType) {
                    MimicType.DNS -> {
                        OutlinedTextField(
                            value = domain,
                            onValueChange = { domain = it },
                            label = { Text(stringResource(R.string.mimic_domain)) },
                            placeholder = { Text("example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    MimicType.QUIC -> {
                        Text(
                            stringResource(R.string.mimic_quic_version),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("1" to "QUIC v1", "2" to "QUIC v2", "draft" to "Draft").forEach { (value, label) ->
                                FilterChip(
                                    selected = quicVersion == value,
                                    onClick = { quicVersion = value },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    MimicType.SIP -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sipFromUser,
                                onValueChange = { sipFromUser = it },
                                label = { Text(stringResource(R.string.mimic_sip_from_user)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = sipToUser,
                                onValueChange = { sipToUser = it },
                                label = { Text(stringResource(R.string.mimic_sip_to_user)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sipFromDomain,
                                onValueChange = { sipFromDomain = it },
                                label = { Text(stringResource(R.string.mimic_sip_from_domain)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = sipToDomain,
                                onValueChange = { sipToDomain = it },
                                label = { Text(stringResource(R.string.mimic_sip_to_domain)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    stringResource(R.string.mimic_itime_settings),
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = itimeMin,
                        onValueChange = { itimeMin = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.mimic_itime_min)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("${MimicSettings.ITIME_MIN_ALLOWED}-${MimicSettings.ITIME_MAX_ALLOWED}") }
                    )
                    OutlinedTextField(
                        value = itimeMax,
                        onValueChange = { itimeMax = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.mimic_itime_max)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("${MimicSettings.ITIME_MIN_ALLOWED}-${MimicSettings.ITIME_MAX_ALLOWED}") }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedTextField(
                    value = regenerateInterval,
                    onValueChange = { regenerateInterval = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.mimic_regenerate_interval)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("${MimicSettings.REGENERATE_MIN}-${MimicSettings.REGENERATE_MAX} sec") }
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val settings = buildSettings()
                    onGenerate(settings)
                }) {
                    Text(stringResource(R.string.mimic_generate))
                }
                TextButton(onClick = {
                    val settings = buildSettings()
                    onApply(settings)
                }) {
                    Text(stringResource(R.string.mimic_apply))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
