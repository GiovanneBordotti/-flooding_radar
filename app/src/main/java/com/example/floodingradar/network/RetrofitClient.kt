package com.example.floodingradar.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 é o endereço especial do emulador Android para acessar o localhost da máquina hospedeira
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val instance: AlertaApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(AlertaApi::class.java)
    }
}

