package com.denie.phraselock

import android.content.Context

/** Избранное и «выученные» фразы — в SharedPreferences. */
class FavoritesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("user_state", Context.MODE_PRIVATE)

    fun favoriteIds(): Set<Int> = readIds(KEY_FAVORITES)
    fun knownIds(): Set<Int> = readIds(KEY_KNOWN)

    fun isFavorite(id: Int): Boolean = id in favoriteIds()
    fun isKnown(id: Int): Boolean = id in knownIds()

    fun toggleFavorite(id: Int) {
        val ids = favoriteIds().toMutableSet()
        if (!ids.add(id)) ids.remove(id)
        writeIds(KEY_FAVORITES, ids)
    }

    fun toggleKnown(id: Int) {
        val ids = knownIds().toMutableSet()
        if (!ids.add(id)) ids.remove(id)
        writeIds(KEY_KNOWN, ids)
    }

    private fun readIds(key: String): Set<Int> =
        prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    private fun writeIds(key: String, ids: Set<Int>) {
        prefs.edit().putStringSet(key, ids.map { it.toString() }.toSet()).apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_KNOWN = "known"
    }
}
