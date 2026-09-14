package com.example.floodingradar.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.floodingradar.viewmodel.MapViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: MapViewModel, onBack: () -> Unit) {
    val alertas by viewModel.alertas.collectAsState()
    
    // State for selected date (defaults to null for "All Time")
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        selectedDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false 
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    selectedDate = null 
                    showDatePicker = false 
                }) {
                    Text("Limpar Filtro")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatório Histórico") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Filtrar por Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        val filteredAlertas = if (selectedDate != null) {
            alertas.filter { it.data_hora?.startsWith(selectedDate!!) == true }
        } else {
            alertas
        }

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Text(
                text = if (selectedDate != null) "Mostrando alertas do dia: $selectedDate" else "Mostrando todos os alertas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (filteredAlertas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum alerta encontrado.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredAlertas.sortedByDescending { it.data_hora }) { p ->
                        val dataFormatada = p.data_hora?.let { dh ->
                            try {
                                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                val date = parser.parse(dh.substringBefore('.'))
                                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                date?.let { formatter.format(it) } ?: "Sem data"
                            } catch (e: Exception) { "Data inválida" }
                        } ?: "Sem data"

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            val context = LocalContext.current
                            val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            val usuarioLogado = sharedPref.getString("nome_usuario", "Anônimo") ?: "Anônimo"

                            Column(modifier = Modifier.padding(12.dp)) {
                                if (p.status == "removido") {
                                    Text(text = "${p.tipo_alerta} [Retirado pelo autor]", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.Gray)
                                    Text(text = "Em: $dataFormatada", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = p.tipo_alerta, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                            if (!p.observacao.isNullOrBlank()) {
                                                Text(text = "Obs: ${p.observacao}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text(text = "Em: $dataFormatada", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "Por: ${p.usuario}", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                                        }
                                        if (p.usuario == usuarioLogado && p.id != null) {
                                            IconButton(onClick = { viewModel.removerAlerta(p.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remover Alerta", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

