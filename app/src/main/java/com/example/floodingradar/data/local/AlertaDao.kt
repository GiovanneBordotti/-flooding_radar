package com.example.floodingradar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertaDao {
    @Insert
    suspend fun insert(alerta: AlertaEntity)

    @Update
    suspend fun update(alerta: AlertaEntity)

    @Query("SELECT * FROM alertas_offline WHERE status = 'pendente'")
    suspend fun getAlertasPendentes(): List<AlertaEntity>

    // Retorna todos usando Flow para atualizar a UI em tempo real
    @Query("SELECT * FROM alertas_offline")
    fun getAllAlertas(): Flow<List<AlertaEntity>>
}

