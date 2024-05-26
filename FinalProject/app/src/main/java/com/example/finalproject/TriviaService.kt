package com.example.finalproject

import retrofit2.http.GET
import retrofit2.http.Query

interface TriviaService {
    @GET("api_token.php?command=request")
    suspend fun getSessionToken() : Token

    @GET("api.php?amount=50&type=multiple&token=")
    suspend fun getQuestions(@Query("sessionToken") sessionToken:String) : Questions
}