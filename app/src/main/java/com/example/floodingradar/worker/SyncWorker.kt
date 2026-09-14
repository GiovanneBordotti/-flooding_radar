package com.example.floodingradar.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.floodingradar.data.local.AppDatabase
import com.example.floodingradar.model.Alerta
import com.example.floodingradar.network.RetrofitClient

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).alertaDao()
        val pendentes = dao.getAlertasPendentes()

        if (pendentes.isEmpty()) return Result.success()

        return try {
            for (alertaLocal in pendentes) {
                // Monta o objeto para a API
                val alertaApi = Alerta(
                    tipo_alerta = alertaLocal.tipo_alerta,
                    latitude = alertaLocal.latitude,
                    longitude = alertaLocal.longitude,
                    observacao = alertaLocal.observacao,
                    usuario = alertaLocal.usuario
                )

                // Envia para o Backend
                RetrofitClient.instance.criarAlerta(alertaApi)

                // Se não der erro (exceção), marca como enviado no Room
                alertaLocal.status = "enviado"
                dao.update(alertaLocal)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Se falhou (sem internet), tenta novamente mais tarde
            Result.retry()
        }
    }
}

