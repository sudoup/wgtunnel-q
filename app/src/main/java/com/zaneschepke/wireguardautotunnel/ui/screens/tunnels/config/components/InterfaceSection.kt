package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wireguard.crypto.KeyPair
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.MimicConfigDialog
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.state.MimicDomainRequiredException
import com.zaneschepke.wireguardautotunnel.ui.state.MimicSettings
import com.zaneschepke.wireguardautotunnel.ui.state.MimicType
import java.util.*

@Composable
fun InterfaceSection(
    isGlobalConfig: Boolean,
    configProxy: ConfigProxy,
    isRunning: Boolean,
    tunnelName: String,
    isTunnelNameTaken: Boolean,
    onInterfaceChange: (InterfaceProxy) -> Unit,
    onTunnelNameChange: (String) -> Unit,
    onMimicQuic: () -> Unit,
    onMimicDns: () -> Unit,
    onMimicSip: () -> Unit,
    mimicDnsSettings: MimicSettings = MimicSettings.defaultDns(),
    mimicQuicSettings: MimicSettings = MimicSettings.defaultQuic(),
    mimicSipSettings: MimicSettings = MimicSettings.defaultSip(),
    onMimicSettingsChange: (MimicSettings) -> Unit = {},
) {
    val isTv = LocalIsAndroidTV.current
    var showAmneziaValues by rememberSaveable {
        mutableStateOf(configProxy.`interface`.isAmneziaEnabled())
    }
    var showPrivateKey by rememberSaveable { mutableStateOf(false) }

    var showScripts by rememberSaveable { mutableStateOf(configProxy.hasScripts()) }
    var isDropDownExpanded by rememberSaveable { mutableStateOf(false) }
    val isAmneziaCompatibilitySet =
        remember(configProxy.`interface`) {
            configProxy.`interface`.isAmneziaCompatibilityModeSet()
        }

    var showMimicDialog by remember { mutableStateOf<MimicType?>(null) }

    fun toggleAmneziaCompat() {
        val (show, interfaceProxy) =
            if (configProxy.`interface`.isAmneziaCompatibilityModeSet()) {
                Pair(false, configProxy.`interface`.resetAmneziaProperties())
            } else Pair(true, configProxy.`interface`.toAmneziaCompatibilityConfig())
        showAmneziaValues = show
        onInterfaceChange(interfaceProxy)
    }

    var mimicError by remember { mutableStateOf<Int?>(null) }

    fun applyMimicSettings(settings: MimicSettings, closeDialog: Boolean = false) {
        configProxy.`interface`.setMimicFromSettings(settings)
            .onSuccess { newInterface ->
                onMimicSettingsChange(settings)
                showAmneziaValues = true
                onInterfaceChange(newInterface)
                mimicError = null
                if (closeDialog) showMimicDialog = null
            }
            .onFailure { e ->
                mimicError = when (e) {
                    is MimicDomainRequiredException -> R.string.mimic_domain_required
                    else -> null
                }
            }
    }

    showMimicDialog?.let { mimicType ->
        val currentSettings = when (mimicType) {
            MimicType.DNS -> mimicDnsSettings
            MimicType.QUIC -> mimicQuicSettings
            MimicType.SIP -> mimicSipSettings
        }
        MimicConfigDialog(
            mimicType = mimicType,
            currentSettings = currentSettings,
            onDismiss = { 
                showMimicDialog = null
                mimicError = null
            },
            onApply = { settings -> applyMimicSettings(settings, closeDialog = true) },
            onGenerate = { settings -> applyMimicSettings(settings, closeDialog = false) },
            errorMessage = mimicError?.let { stringResource(it) },
        )
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!isGlobalConfig)
                    GroupLabel(
                        stringResource(R.string.interface_),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                if (!isGlobalConfig)
                    Row {
                        if (isTv) {
                            IconButton(onClick = { showPrivateKey = !showPrivateKey }) {
                                Icon(
                                    Icons.Outlined.RemoveRedEye,
                                    stringResource(R.string.show_password),
                                )
                            }
                            IconButton(
                                enabled = true,
                                onClick = {
                                    val keypair = KeyPair()
                                    onInterfaceChange(
                                        configProxy.`interface`.copy(
                                            privateKey = keypair.privateKey.toBase64(),
                                            publicKey = keypair.publicKey.toBase64(),
                                        )
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    stringResource(R.string.rotate_keys),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        InterfaceDropdown(
                            expanded = isDropDownExpanded,
                            onExpandedChange = { isDropDownExpanded = it },
                            showScripts = showScripts,
                            showAmneziaValues = showAmneziaValues,
                            isAmneziaCompatibilitySet = isAmneziaCompatibilitySet,
                            onToggleScripts = { showScripts = !showScripts },
                            onToggleAmneziaValues = { showAmneziaValues = !showAmneziaValues },
                            onToggleAmneziaCompatibility = { toggleAmneziaCompat() },
                            onMimicQuic = {
                                showAmneziaValues = true
                                onMimicQuic()
                            },
                            onMimicDns = {
                                showAmneziaValues = true
                                onMimicDns()
                            },
                            onMimicSip = {
                                showAmneziaValues = true
                                onMimicSip()
                            },
                            onMimicQuicSettings = { showMimicDialog = MimicType.QUIC },
                            onMimicDnsSettings = { showMimicDialog = MimicType.DNS },
                            onMimicSipSettings = { showMimicDialog = MimicType.SIP },
                        )
                    }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                if (!isGlobalConfig)
                    ConfigurationTextBox(
                        value = tunnelName,
                        enabled = !isRunning,
                        onValueChange = onTunnelNameChange,
                        label = stringResource(R.string.name),
                        isError = isTunnelNameTaken,
                        supportingText =
                            if (isRunning) {
                                {
                                    DescriptionText(
                                        stringResource(R.string.tunnel_running_name_message)
                                    )
                                }
                            } else null,
                        hint =
                            stringResource(
                                    R.string.hint_template,
                                    stringResource(R.string.tunnel_name),
                                )
                                .lowercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                    )
                InterfaceFields(
                    isGlobalConfig,
                    interfaceState = configProxy.`interface`,
                    showScripts = showScripts,
                    showAmneziaValues = showAmneziaValues,
                    onInterfaceChange = onInterfaceChange,
                    showPrivateKey,
                )
            }
        }
    }
}
