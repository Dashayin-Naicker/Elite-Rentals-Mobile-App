package com.rentals.eliterentals

import android.util.Log
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

object RetrofitClient {

    private const val BASE_URL =
        "https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/"

    // Logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Connection interceptor to set headers
    private val connectionInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Connection", "keep-alive") // Keep alive for connection pooling
            .build()
        chain.proceed(request)
    }

    // Error interceptor to handle SSL/TLS quirks
    private val errorInterceptor = Interceptor { chain ->
        try {
            val response = chain.proceed(chain.request())
            // Avoid EOFException for empty responses
            if (response.body == null || response.code == 204) {
                return@Interceptor response.newBuilder()
                    .body(ResponseBody.create(null, ""))
                    .build()
            }
            response
        } catch (e: SSLProtocolException) {
            Log.w("RetrofitClient", "Ignored SSLProtocolException: ${e.message}")
            throw IOException("Network closed unexpectedly (non-fatal)")
        } catch (e: IOException) {
            // Optionally log for retry debugging
            Log.w("RetrofitClient", "IOException during request: ${e.message}")
            throw e
        }
    }

    // CookieJar to persist ARRAffinity cookies for Azure Web App
    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    // Trust all SSL certificates (for development only)
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    // OkHttp client with connection pool, retries, and interceptors
    private val client = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(true)
        .cookieJar(cookieJar)
        .addInterceptor(connectionInterceptor)
        .addInterceptor(errorInterceptor)
        .addInterceptor(loggingInterceptor)
        .protocols(listOf(Protocol.HTTP_1_1)) // Force HTTP/1.1 for Azure TLS stability
        .build()

    // Gson instance for lenient parsing
    private val gson = com.google.gson.GsonBuilder()
        .setLenient()
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
