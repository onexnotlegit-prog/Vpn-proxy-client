package com.example.vpnclient

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vpnclient.data.VpnServer
import com.example.vpnclient.ui.components.LogConsole
import com.example.vpnclient.ui.components.ServerCard
import com.example.vpnclient.ui.theme.VpnProxyClientTheme
import com.example.vpnclient.vpn.VpnConnectionState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VpnProxyClientTheme {
                VpnScreen(
                    onNeedVpnPermission = { requestVpnPermission() },
                    viewModel = viewModel
                )
            }
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VpnScreen(
    onNeedVpnPermission: () -> Unit,
    viewModel: MainViewModel
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    var subscriptionDialog by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<VpnServer?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (vpnState == VpnConnectionState.CONNECTED || vpnState == VpnConnectionState.CONNECTING) {
                    viewModel.disconnect()
                } else {
                    selectedServer?.let {
                        onNeedVpnPermission()
                        viewModel.connect(it)
                    }
                }
            }) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "connect")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectionIndicator(vpnState)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.addFromClipboard() }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null)
                    Text(" Вставить")
                }
                Button(onClick = { subscriptionDialog = true }) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Text(" Подписки")
                }
            }

            Text("Серверы", fontFamily = FontFamily.Monospace)

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(servers, key = { it.id }) { server ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteServer(server)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                        content = {
                            ServerCard(server = server, onClick = {
                                selectedServer = server
                                if (vpnState == VpnConnectionState.CONNECTED) {
                                    onNeedVpnPermission()
                                    viewModel.switch(server)
                                }
                            })
                        }
                    )
                }
            }

            LogConsole(logs = logs)
        }
    }

    if (subscriptionDialog) {
        SubscriptionDialog(
            onDismiss = { subscriptionDialog = false },
            onConfirm = {
                viewModel.addSubscription(it)
                subscriptionDialog = false
            }
        )
    }
}

@Composable
private fun ConnectionIndicator(state: VpnConnectionState) {
    val transition = rememberInfiniteTransition(label = "connection")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val color = when (state) {
        VpnConnectionState.CONNECTED -> Color(0xFF53D769)
        VpnConnectionState.CONNECTING -> Color(0xFFFFCC00)
        VpnConnectionState.ERROR -> Color(0xFFFF5F56)
        VpnConnectionState.DISCONNECTED -> Color(0xFF8E8E93)
    }

    Text(
        text = "Статус: $state",
        color = color,
        modifier = Modifier.alpha(alpha),
        fontFamily = FontFamily.Monospace
    )
}

@Composable
private fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить подписку", fontFamily = FontFamily.Monospace) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("URL") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            IconButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
