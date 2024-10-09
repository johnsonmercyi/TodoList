package com.example.todolist.dao

import com.example.todolist.entity.QuoteResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface QuoteApiService {
    @GET("qod?category=inspire")
    suspend fun getQuoteOfTheDay(
        @Header("X-Theysaidso-Api-Secret") apiKey: String
    ): QuoteResponse
}