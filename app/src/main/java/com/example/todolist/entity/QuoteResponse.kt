package com.example.todolist.entity

data class QuoteResponse(
    val success: Success,
    val contents: Contents
)

data class Success(
    val total: Int
)

data class Contents(
    val quotes: List<Quote>
)

data class Quote(
    val quote: String,
    val author: String,
    val category: String,
    val length: String,
    val tags: List<String>
)
