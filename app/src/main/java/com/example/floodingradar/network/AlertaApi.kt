package com.example.floodingradar.network

import com.example.floodingradar.model.Alerta
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AlertaApi {
    @GET("alertas")
    suspend fun getAlertas(): List<Alerta>

    @POST("alertas")
    suspend fun criarAlerta(@Body alerta: Alerta): Alerta
}

