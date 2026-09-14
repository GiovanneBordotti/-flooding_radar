package com.example.floodingradar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alertas_offline")
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tipo_alerta: String,
    val latitude: Double,
    val longitude: Double,
    val observacao: String? = null,
    val usuario: String = "Anônimo",
    var status: String = "pendente", // pendente, enviado
    val data_hora: String = ""
)
