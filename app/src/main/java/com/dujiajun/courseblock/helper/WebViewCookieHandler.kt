package com.dujiajun.courseblock.helper

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class WebViewCookieHandler : CookieJar {
    private val mCookieManager = CookieManager.getInstance()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()
        for (cookie in cookies) {
            mCookieManager.setCookie(urlString, cookie.toString())
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val urlString = url.toString()
        val cookiesString = mCookieManager.getCookie(urlString)
        if (cookiesString != null && !cookiesString.isEmpty()) {
            val cookieHeaders =
                cookiesString.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val cookies: MutableList<Cookie> = ArrayList(cookieHeaders.size)
            for (header in cookieHeaders) {
                Cookie.parse(url, header)?.let { cookies.add(it) }
            }
            return cookies
        }
        return emptyList()
    }
}