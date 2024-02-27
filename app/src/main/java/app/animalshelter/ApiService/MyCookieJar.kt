package app.animalshelter.ApiService

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class MyCookieJar : CookieJar {
    private var cookies: List<Cookie> = ArrayList()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies
    }

    fun clear() {
        cookies = ArrayList()
    }

    fun printCookiesToLog() {
        if (cookies.isEmpty()) {
            Log.i("Cookies", "No cookies")
            return
        }
        var cookieList: String = ""
        for (cookie in cookies) {
            cookieList += "\n${cookie.name}: \"${cookie.value}\""
        }
        Log.i("Cookies", "Cookies:" + cookieList)
    }
}