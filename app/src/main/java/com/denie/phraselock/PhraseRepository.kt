package com.denie.phraselock

import android.content.Context
import org.json.JSONObject

data class Phrase(
    val id: Int,
    val en: String,
    val ru: String,
    val category: String,
    val level: String
)

/** База фраз из assets/phrases.json + детерминированный выбор «фразы часа». */
object PhraseRepository {

    @Volatile
    private var cache: List<Phrase>? = null

    fun phrases(context: Context): List<Phrase> {
        return cache ?: synchronized(this) {
            cache ?: parse(context.applicationContext).also { cache = it }
        }
    }

    fun phraseForHour(context: Context, millis: Long): Phrase {
        val list = phrases(context)
        if (list.isEmpty()) {
            return Phrase(0, "Nice to meet you", "Приятно познакомиться", "smalltalk", "A1")
        }
        val bucket = millis / 3_600_000L
        val index = (splitmix64(bucket.toULong()) % list.size.toULong()).toInt()
        return list[index]
    }

    fun categoryTitles(context: Context): Map<String, Pair<String, String>> {
        val json = context.applicationContext.assets.open("phrases.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val cats = root.getJSONArray("categories")
        val map = LinkedHashMap<String, Pair<String, String>>()
        for (i in 0 until cats.length()) {
            val c = cats.getJSONObject(i)
            // id -> (emoji, title)
            map[c.getString("id")] = c.optString("emoji", "") to c.getString("title")
        }
        return map
    }

    private fun parse(context: Context): List<Phrase> {
        val json = context.assets.open("phrases.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val arr = root.getJSONArray("phrases")
        val result = ArrayList<Phrase>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                Phrase(
                    id = o.getInt("id"),
                    en = o.getString("en"),
                    ru = o.getString("ru"),
                    category = o.getString("category"),
                    level = o.getString("level")
                )
            )
        }
        return result
    }

    private fun splitmix64(seed: ULong): ULong {
        var z = seed + 0x9E3779B97F4A7C15uL
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return z xor (z shr 31)
    }
}
