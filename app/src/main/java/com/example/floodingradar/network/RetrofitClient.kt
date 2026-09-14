package com.example.floodingradar.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // URL pública gerada pelo Ngrok conectando os celulares do mundo todo ao seu PC local
    private const val BASE_URL = "https://unnatural-slacking-enlighten.ngrok-free.dev/"

    val instance: AlertaApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(AlertaApi::class.java)
    }
}

