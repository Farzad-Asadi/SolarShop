package com.example.solarShop.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder


object JsonExt {
    val gson: Gson = GsonBuilder().serializeNulls().create()


    inline fun <reified T> fromJson(json: String): T = gson.fromJson(json, T::class.java)
    fun toJson(value: Any): String = gson.toJson(value)
}