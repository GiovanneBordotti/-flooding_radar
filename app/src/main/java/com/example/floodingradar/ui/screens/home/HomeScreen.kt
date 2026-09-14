package com.example.floodingradar.ui.screens.home

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNavigateToMap: () -> Unit) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    // Lê o nome salvo (se existir)
    var nome by remember { mutableStateOf(sharedPref.getString("nome_usuario", "") ?: "") }

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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Salva o nome
                    val nomeSalvar = nome.ifBlank { "Anônimo" }
                    with (sharedPref.edit()) {
                        putString("nome_usuario", nomeSalvar)
                        apply()
                    }
                    onNavigateToMap()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Entrar no Mapa", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

