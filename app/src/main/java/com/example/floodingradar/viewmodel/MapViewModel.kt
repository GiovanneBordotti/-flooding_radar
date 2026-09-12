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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).alertaDao()
    private val workManager = WorkManager.getInstance(application)

    private val _alertas = MutableStateFlow<List<Alerta>>(emptyList())
    val alertas: StateFlow<List<Alerta>> = _alertas

    fun buscarAlertas() {
        viewModelScope.launch {
            try {
                // Tenta puxar do servidor
                val response = RetrofitClient.instance.getAlertas()
                _alertas.value = response
            } catch (e: Exception) {
                // Se falhar (offline), puxa do banco local
                e.printStackTrace()
                dao.getAllAlertas().collect { locais ->
                    _alertas.value = locais.map { 
                        Alerta(tipo_alerta = it.tipo_alerta, latitude = it.latitude, longitude = it.longitude, observacao = it.observacao, status = it.status) 
                    }
                }
            }
        }
    }

    fun enviarAlerta(tipo: String, lat: Double, lng: Double, observacao: String) {
        viewModelScope.launch {
            // 1. Salva no Room PRIMEIRO (Status Pendente)
            val entity = AlertaEntity(
                tipo_alerta = tipo,
                latitude = lat,
                longitude = lng,
                observacao = observacao.takeIf { it.isNotBlank() }
            )
            dao.insert(entity)

            // 2. Agenda o Worker para tentar enviar (só quando tiver Internet)
            agendarSyncWorker()
            
            // 3. Atualiza a tela
            buscarAlertas()
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
}


