package com.example.floodingradar.model

data class Alerta(
    val id: Int? = null,
    val tipo_alerta: String,
    val latitude: Double,
    val longitude: Double,
    val observacao: String? = null,
    val data_hora: String? = null,
    val status: String? = "pendente"
)

