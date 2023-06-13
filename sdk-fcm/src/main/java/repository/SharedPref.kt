package repository

import android.content.Context
import sdkFcm.SHARED_PREFERENCE_FILE_NAME


internal class SharedPref( context: Context) {

    private val pref = context.getSharedPreferences(SHARED_PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)

    internal fun putString(key: String, value: String?) {
        pref.edit().putString(key, value).apply()
    }

    internal fun getString(key: String, defaultValue: String): String? {
        return pref.getString(key, defaultValue)
    }
}
