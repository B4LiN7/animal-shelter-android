package app.animalshelter.ApiService

import android.content.Context
import android.util.Log
import app.animalshelter.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitService {
    lateinit var BASE_URL: String
    lateinit var cookieJar: MyCookieJar

    fun initialize(context: Context) {
        Log.i("RetrofitService", "Initializing RetrofitService...")
        BASE_URL = context.resources.getString(R.string.base_url)
        Log.i("RetrofitService", "BASE_URL set to: $BASE_URL")
        cookieJar = MyCookieJar(context)
        Log.i("RetrofitService", "CookieJar initialized")
        printCookiesToLog()
    }

    fun getRetrofitService(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder()
        client.addInterceptor(logging)
        client.cookieJar(cookieJar)

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun printCookiesToLog() {
        val cookies = cookieJar.getCookies()
        if (cookies.isEmpty()) {
            Log.i("RetrofitService", "No cookies")
            return
        }
        var cookieList: String = ""
        for (cookie in cookies) {
            cookieList += "\n${cookie.name}: \"${cookie.value}\""
        }
        Log.i("RetrofitService", "Cookies:$cookieList")
    }
}