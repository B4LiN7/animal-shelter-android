package app.animalshelter.api

import android.content.Context
import app.animalshelter.R
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.net.URL

class RetrofitCookieJar(private val context: Context) : CookieJar {
    private var cookies: List<Cookie> = ArrayList()

    init {
        val sharedPreferences = context.getSharedPreferences("RetrofitCookies", Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all
        val url = URL(context.resources.getString(R.string.base_url))
        val domain = url.host
        for ((key, value) in allEntries.entries) {
            cookies += Cookie.Builder().name(key).value(value as String).domain(domain).build()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies = cookies
        val sharedPreferences = context.getSharedPreferences("RetrofitCookies", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        for (cookie in cookies) {
            editor.putString(cookie.name, cookie.value)
        }
        editor.apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies
    }

    fun clearCookies() {
        cookies = ArrayList()
        val sharedPreferences = context.getSharedPreferences("RetrofitCookies", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    fun getCookies(): List<Cookie> {
        return cookies
    }
}