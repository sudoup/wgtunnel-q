package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.LocalSharedVm
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.security.SecureScreenFromRecording
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components.AddPeerButton
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components.InterfaceSection
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.config.components.PeersSection
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.ui.state.ConfigProxy
import com.zaneschepke.wireguardautotunnel.ui.state.MimicGenerator
import com.zaneschepke.wireguardautotunnel.ui.state.MimicSettings
import com.zaneschepke.wireguardautotunnel.ui.state.MimicType
import com.zaneschepke.wireguardautotunnel.ui.state.PeerProxy
import com.zaneschepke.wireguardautotunnel.viewmodel.ConfigViewModel
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectSideEffect

private val android.content.Context.mimicDataStore by preferencesDataStore(name = "mimic_settings")

@Composable
fun ConfigScreen(viewModel: ConfigViewModel) {
    val sharedViewModel = LocalSharedVm.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    if (uiState.isLoading) return

    val locale = remember { Locale.getDefault() }

    var configProxy by remember {
        mutableStateOf(uiState.tunnel?.let { ConfigProxy.from(it.toAmConfig()) } ?: ConfigProxy())
    }

    var tunnelName by remember { mutableStateOf(uiState.tunnel?.name ?: "") }
    val isGlobalConfig = rememberSaveable { tunnelName == TunnelConfig.GLOBAL_CONFIG_NAME }

    val isTunnelNameTaken by
        remember(tunnelName) { derivedStateOf { uiState.unavailableNames.contains(tunnelName) } }

    var mimicDnsSettings by rememberSaveable(stateSaver = MimicSettings.Saver) { mutableStateOf(MimicSettings.defaultDns()) }
    var mimicQuicSettings by rememberSaveable(stateSaver = MimicSettings.Saver) { mutableStateOf(MimicSettings.defaultQuic()) }
    var mimicSipSettings by rememberSaveable(stateSaver = MimicSettings.Saver) { mutableStateOf(MimicSettings.defaultSip()) }

    var activeMimicType by remember { mutableStateOf<MimicType?>(null) }

    LaunchedEffect(Unit) {
        val dnsJson = context.mimicDataStore.data.map { it[DataStoreManager.mimicDnsSettings] }.first()
        val quicJson = context.mimicDataStore.data.map { it[DataStoreManager.mimicQuicSettings] }.first()
        val sipJson = context.mimicDataStore.data.map { it[DataStoreManager.mimicSipSettings] }.first()

        dnsJson?.let { MimicSettings.fromJson(it)?.let { s -> mimicDnsSettings = s } }
        quicJson?.let { MimicSettings.fromJson(it)?.let { s -> mimicQuicSettings = s } }
        sipJson?.let { MimicSettings.fromJson(it)?.let { s -> mimicSipSettings = s } }
    }

    LaunchedEffect(activeMimicType, mimicDnsSettings, mimicQuicSettings, mimicSipSettings) {
        val type = activeMimicType ?: return@LaunchedEffect
        val settings = when (type) {
            MimicType.DNS -> mimicDnsSettings
            MimicType.QUIC -> mimicQuicSettings
            MimicType.SIP -> mimicSipSettings
        }
        val intervalMs = settings.regenerateIntervalSeconds * 1000L
        if (intervalMs > 0) {
            while (true) {
                delay(intervalMs)
                val newResult = MimicGenerator.generate(settings)
                configProxy = configProxy.copy(`interface` = configProxy.`interface`.applyMimicResult(newResult))
            }
        }
    }

    fun saveMimicSettings(settings: MimicSettings) {
        scope.launch {
            val key = when (settings.type) {
                MimicType.DNS -> DataStoreManager.mimicDnsSettings
                MimicType.QUIC -> DataStoreManager.mimicQuicSettings
                MimicType.SIP -> DataStoreManager.mimicSipSettings
            }
            context.mimicDataStore.edit { it[key] = settings.toJson() }
        }
    }

    sharedViewModel.collectSideEffect { sideEffect ->
        if (sideEffect is LocalSideEffect.SaveChanges)
            if (uiState.isRunning) viewModel.setShowSaveModal(true)
            else viewModel.saveConfigProxy(configProxy, tunnelName)
    }

    if (uiState.showSaveModal) {
        InfoDialog(
            onDismiss = { viewModel.setShowSaveModal(false) },
            onAttest = { viewModel.saveConfigProxy(configProxy, tunnelName) },
            title = stringResource(R.string.save_changes),
            body = {
                Text(
                    stringResource(
                        R.string.restart_message_template,
                        stringResource(R.string.tunnels).lowercase(locale),
                    )
                )
            },
            confirmText = stringResource(R.string._continue),
        )
    }

    SecureScreenFromRecording()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        InterfaceSection(
            isGlobalConfig,
            configProxy = configProxy,
            uiState.isRunning,
            tunnelName,
            isTunnelNameTaken,
            onInterfaceChange = { configProxy = configProxy.copy(`interface` = it) },
            onTunnelNameChange = { tunnelName = it },
            onMimicQuic = {
                activeMimicType = MimicType.QUIC
                configProxy = configProxy.copy(`interface` = configProxy.`interface`.setMimicFromSettings(mimicQuicSettings))
            },
            onMimicDns = {
                activeMimicType = MimicType.DNS
                configProxy = configProxy.copy(`interface` = configProxy.`interface`.setMimicFromSettings(mimicDnsSettings))
            },
            onMimicSip = {
                activeMimicType = MimicType.SIP
                configProxy = configProxy.copy(`interface` = configProxy.`interface`.setMimicFromSettings(mimicSipSettings))
            },
            mimicDnsSettings = mimicDnsSettings,
            mimicQuicSettings = mimicQuicSettings,
            mimicSipSettings = mimicSipSettings,
            onMimicSettingsChange = { settings ->
                when (settings.type) {
                    MimicType.DNS -> mimicDnsSettings = settings
                    MimicType.QUIC -> mimicQuicSettings = settings
                    MimicType.SIP -> mimicSipSettings = settings
                }
                activeMimicType = settings.type
                saveMimicSettings(settings)
            },
        )
        if (!isGlobalConfig)
            PeersSection(
                configProxy,
                onRemove = {
                    configProxy =
                        configProxy.copy(
                            peers = configProxy.peers.toMutableList().apply { removeAt(it) }
                        )
                },
                onToggleLan = { index ->
                    configProxy =
                        configProxy.copy(
                            peers =
                                configProxy.peers.toMutableList().apply {
                                    val peer = get(index)
                                    val updated =
                                        if (peer.isLanExcluded()) peer.includeLan()
                                        else peer.excludeLan()
                                    set(index, updated)
                                }
                        )
                },
                onUpdatePeer = { peer, index ->
                    configProxy =
                        configProxy.copy(
                            peers = configProxy.peers.toMutableList().apply { set(index, peer) }
                        )
                },
            )
        if (!isGlobalConfig)
            AddPeerButton {
                configProxy = configProxy.copy(peers = configProxy.peers + PeerProxy())
            }
    }
}
