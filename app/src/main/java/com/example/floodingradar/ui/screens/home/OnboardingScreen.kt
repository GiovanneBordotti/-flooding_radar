package com.example.floodingradar.ui.screens.home

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onNavigateToMap: () -> Unit) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    var nome by remember { mutableStateOf(sharedPref.getString("nome_usuario", "") ?: "") }
    var aceitouTermos by remember { mutableStateOf(sharedPref.getBoolean("aceitou_termos", false)) }
    var mostrarTermos by remember { mutableStateOf(false) }

    if (mostrarTermos) {
        AlertDialog(
            onDismissRequest = { mostrarTermos = false },
            title = { Text("Termos de Uso e Privacidade") },
            text = {
                Text("De acordo com a Lei Geral de Proteção de Dados (LGPD), informamos que o Flooding Radar coleta e armazena a sua geolocalização exata estritamente para o propósito de registrar e notificar desastres naturais (alagamentos e quedas de árvores) próximos a você. Seu apelido será salvo publicamente para identificar a autoria dos alertas. Você pode apagar seus alertas a qualquer momento. Ao continuar, você consente com o uso destes dados.")
            },
            confirmButton = {
                TextButton(onClick = { mostrarTermos = false }) {
                    Text("Entendi")
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bem-vindo ao",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Flooding Radar",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Qual o seu nome?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = aceitouTermos,
                    onCheckedChange = { aceitouTermos = it }
                )
                Text(
                    text = "Li e concordo com a Política de Privacidade e consentimento de Localização.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { mostrarTermos = true }.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val nomeSalvar = nome.ifBlank { "Anônimo" }
                    with (sharedPref.edit()) {
                        putString("nome_usuario", nomeSalvar)
                        putBoolean("aceitou_termos", aceitouTermos)
                        apply()
                    }
                    onNavigateToMap()
                },
                enabled = aceitouTermos,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Entrar no Mapa", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

