package com.obill.app.data.remote

import com.obill.app.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor

fun createLoggingInterceptor(): HttpLoggingInterceptor {
    val logging = HttpLoggingInterceptor()
    logging.level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
    return logging
}
