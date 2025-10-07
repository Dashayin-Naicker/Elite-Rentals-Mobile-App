package com.rentals.eliterentals

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object RetrofitClient {
    //private const val BASE_URL = "http://10.0.2.2:5263/"

    private const val BASE_URL = "https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/"



    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }


    private val connectionInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Connection", "close")
            .build()
        chain.proceed(request)
    }

    private val errorInterceptor = Interceptor { chain ->
        try {
            val response = chain.proceed(chain.request())
            // Wrap empty responses to avoid EOFException
            if (response.body == null || response.code == 204) {
                return@Interceptor response.newBuilder()
                    .body(okhttp3.ResponseBody.create(null, ""))
                    .build()
            }
            response
        } catch (e: javax.net.ssl.SSLProtocolException) {
            // Ignore harmless SSL close errors (BAD_DECRYPT)
            android.util.Log.w("RetrofitClient", "Ignored SSLProtocolException: ${e.message}")
            throw java.io.IOException("Network closed unexpectedly (non-fatal)")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }



    // Trust all SSL certificates (for dev only)
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    private val client = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .addInterceptor(connectionInterceptor)
        .addInterceptor(errorInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val gson = com.google.gson.GsonBuilder()
        .setLenient() // <-- allows incomplete/malformed JSON
        .create()


    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
