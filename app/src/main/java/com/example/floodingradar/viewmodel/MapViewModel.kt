package com.example.floodingradar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.floodingradar.data.local.AlertaEntity
import com.example.floodingradar.data.local.AppDatabase
import com.example.floodingradar.model.Alerta
import com.example.floodingradar.network.RetrofitClient
import com.example.floodingradar.worker.SyncWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).alertaDao()
    private val workManager = WorkManager.getInstance(application)

    private val _alertas = MutableStateFlow<List<Alerta>>(emptyList())
    val alertas: StateFlow<List<Alerta>> = _alertas

    private val _mensagensUi = MutableSharedFlow<String>()
    val mensagensUi: SharedFlow<String> = _mensagensUi.asSharedFlow()

    fun buscarAlertas() {
        viewModelScope.launch {
            try {
                // Tenta puxar do servidor
                val response = RetrofitClient.instance.getAlertas()
                
                // Pega os que ainda estão pendentes localmente para não sumirem da tela
                val pendentes = dao.getAlertasPendentes().map {
                    Alerta(tipo_alerta = it.tipo_alerta, latitude = it.latitude, longitude = it.longitude, observacao = it.observacao, data_hora = it.data_hora, usuario = it.usuario, status = it.status) 
                }
                
                _alertas.value = response + pendentes
            } catch (e: Exception) {
                // Se falhar (offline), puxa do banco local
                e.printStackTrace()
                val locais = dao.getAllAlertas().first()
                _alertas.value = locais.map { 
                    Alerta(tipo_alerta = it.tipo_alerta, latitude = it.latitude, longitude = it.longitude, observacao = it.observacao, data_hora = it.data_hora, usuario = it.usuario, status = it.status) 
                }
            }
        }
    }

    fun enviarAlerta(tipo: String, lat: Double, lng: Double, observacao: String, usuario: String) {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.getDefault())
            val dataHoraAtual = sdf.format(java.util.Date())

            // 1. Salva no Room PRIMEIRO (Status Pendente)
            val entity = AlertaEntity(
                tipo_alerta = tipo,
                latitude = lat,
                longitude = lng,
                observacao = observacao.takeIf { it.isNotBlank() },
                usuario = usuario,
                data_hora = dataHoraAtual
            )
            dao.insert(entity)

            // 2. Agenda o Worker para tentar enviar (só quando tiver Internet)
            agendarSyncWorker()
            
            // Avisa o usuário sobre o modo Offline
            _mensagensUi.emit("Alerta salvo! Será enviado à nuvem assim que houver rede.")

            // 3. Atualiza a tela localmente na hora (evita a condição de corrida do RabbitMQ onde a API ainda não processou o Worker)
            val novoAlerta = Alerta(
                tipo_alerta = tipo,
                latitude = lat,
                longitude = lng,
                observacao = observacao.takeIf { it.isNotBlank() },
                data_hora = dataHoraAtual,
                usuario = usuario,
                status = "pendente"
            )
            _alertas.value = _alertas.value + novoAlerta
        }
    }

    private fun agendarSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(syncRequest)
    }

    fun removerAlerta(id: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.removerAlerta(id)
                buscarAlertas()
            } catch (e: Exception) {
                e.printStackTrace()
                _mensagensUi.emit("Erro ao remover o alerta.")
            }
        }
    }
}


