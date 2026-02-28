package com.example.dailydeals.ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun onAttach(context: Context): Context {
        val lang = getPersistedData(context, "en")
        return setLocale(context, lang)
    }

    fun getPersistedData(context: Context, defaultLanguage: String): String {
        val preferences = context.getSharedPreferences("daily_deals_prefs", Context.MODE_PRIVATE)
        return preferences.getString("language", defaultLanguage) ?: defaultLanguage
    }

    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }

    private fun persist(context: Context, language: String) {
        val preferences = context.getSharedPreferences("daily_deals_prefs", Context.MODE_PRIVATE)
        preferences.edit().putString("language", language).apply()
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }
}
