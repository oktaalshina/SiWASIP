package com.example.siwasip.data.local

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private const val PREF_NAME = "siwasis_prefs"
    private const val KEY_TOKEN = "auth_token"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    var authToken: String?
        get() = prefs?.getString(KEY_TOKEN, null)
        set(value) {
            prefs?.edit()?.putString(KEY_TOKEN, value)?.apply()
        }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}