package com.example.floodingradar.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
    
    var nomeUsuario by remember { mutableStateOf(sharedPref.getString("nome_usuario", "") ?: "") }
    var notificacoesAtivadas by remember { mutableStateOf(sharedPref.getBoolean("notificacoes_ativadas", true)) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Perfil", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            OutlinedTextField(
                value = nomeUsuario,
                onValueChange = { nomeUsuario = it },
                label = { Text("Seu Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = {
                    sharedPref.edit().putString("nome_usuario", nomeUsuario).apply()
                    scope.launch { snackbarHostState.showSnackbar("Nome atualizado com sucesso!") }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Salvar Nome")
            }

            HorizontalDivider()

            Text("Notificações Push", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            var notificacoesRaioApenas by remember { mutableStateOf(sharedPref.getBoolean("notificacoes_raio_apenas", true)) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Receber Alertas", style = MaterialTheme.typography.bodyLarge)
                    Text("Seja avisado quando novos alagamentos ou quedas de árvores forem reportados.", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                }
                Switch(
                    checked = notificacoesAtivadas,
                    onCheckedChange = { isChecked ->
                        notificacoesAtivadas = isChecked
                        sharedPref.edit().putBoolean("notificacoes_ativadas", isChecked).apply()
                        
                        if (isChecked) {
                            FirebaseMessaging.getInstance().subscribeToTopic("alertas")
                        } else {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("alertas")
                        }
                    }
                )
            }

            if (notificacoesAtivadas) {
                var raioKm by remember { mutableStateOf(sharedPref.getInt("notificacoes_raio_km", 3).toFloat()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Filtrar por Distância", style = MaterialTheme.typography.bodyLarge)
                        Text("Silencia notificações de ocorrências muito distantes de você.", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                    }
                    Switch(
                        checked = notificacoesRaioApenas,
                        onCheckedChange = { isChecked ->
                            notificacoesRaioApenas = isChecked
                            sharedPref.edit().putBoolean("notificacoes_raio_apenas", isChecked).apply()
                        }
                    )
                }
                
                if (notificacoesRaioApenas) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Raio do Radar: ${raioKm.toInt()} km", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Slider(
                            value = raioKm,
                            onValueChange = { novoValor ->
                                raioKm = novoValor
                            },
                            onValueChangeFinished = {
                                sharedPref.edit().putInt("notificacoes_raio_km", raioKm.toInt()).apply()
                            },
                            valueRange = 1f..10f,
                            steps = 8 // 1 a 10
                        )
                    }
                }
            }
        }
    }
}
