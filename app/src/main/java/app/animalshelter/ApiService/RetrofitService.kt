package app.animalshelter.ApiService

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitService {
    val BASE_URL = "http://10.0.2.2:3001/"

    val cookieJar = MyCookieJar()

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
}

class MyCookieJar : CookieJar {
    private var cookies: List<Cookie> = ArrayList()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies
    }

    fun getCookies(): List<Cookie> {
        return cookies
    }

    fun printCookies() {
        Log.i("Cookies", "Cookies:")
        for (cookie in cookies) {
            Log.i("Cookies", "${cookie.name}: \"${cookie.value}\"")
        }
    }
}