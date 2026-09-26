package com.right9code.anyhome.data

import android.content.Context
import org.json.JSONArray
import java.util.Random

object QuotesManager {

    val DEFAULT_QUOTES = listOf(
        "\"A room without books is like a body without a soul.\" — Cicero",
        "\"You have power over your mind - not outside events. Realize this, and you will find strength.\" — Marcus Aurelius",
        "\"We suffer more often in imagination than in reality.\" — Seneca",
        "\"To live is the rarest thing in the world. Most people exist, that is all.\" — Oscar Wilde",
        "\"It is not death that a man should fear, but he should fear never beginning to live.\" — Marcus Aurelius",
        "\"The only true wisdom is in knowing you know nothing.\" — Socrates",
        "\"Waste no more time arguing what a good man should be. Be one.\" — Marcus Aurelius",
        "\"He who has a why to live can bear almost any how.\" — Friedrich Nietzsche",
        "\"No man is free who is not master of himself.\" — Epictetus",
        "\"The journey of a thousand miles begins with one step.\" — Lao Tzu",
        "\"In the midst of chaos, there is also opportunity.\" — Sun Tzu",
        "\"There is no friend as loyal as a book.\" — Ernest Hemingway",
        "\"I cannot live without books.\" — Thomas Jefferson",
        "\"Books are a uniquely portable magic.\" — Stephen King",
        "\"Think before you speak. Read before you think.\" — Fran Lebowitz",
        "\"So many books, so little time.\" — Frank Zappa",
        "\"The mind, once stretched by a new idea, never returns to its original dimensions.\" — Ralph Waldo Emerson",
        "\"Not all those who wander are lost.\" — J.R.R. Tolkien",
        "\"Knowing yourself is the beginning of all wisdom.\" — Aristotle",
        "\"Silence is a source of great strength.\" — Lao Tzu",
        "\"Happiness depends upon ourselves.\" — Aristotle",
        "\"Somewhere, something incredible is waiting to be known.\" — Carl Sagan",
        "\"Those who can imagine anything, can create the impossible.\" — Alan Turing",
        "\"It is never too late to be what you might have been.\" — George Eliot",
        "\"Life isn't about finding yourself. Life is about creating yourself.\" — George Bernard Shaw",
        "\"Do what you can, with what you have, where you are.\" — Theodore Roosevelt",
        "\"Everything has beauty, but not everyone sees it.\" — Confucius",
        "\"Simplicity is the ultimate sophistication.\" — Leonardo da Vinci",
        "\"The soul becomes dyed with the color of its thoughts.\" — Marcus Aurelius",
        "\"If you are distressed by anything external, the pain is not due to the thing itself, but to your estimate of it.\" — Marcus Aurelius",
        "\"Life is very short and there's no time for fussing and fighting, my friend.\" — John Lennon",
        "\"Be curious, not judgmental.\" — Walt Whitman",
        "\"Whatever you are, be a good one.\" — Abraham Lincoln",
        "\"An unexamined life is not worth living.\" — Socrates",
        "\"The unexamined reading is not worth doing.\" — Mortimer J. Adler",
        "\"Turn your wounds into wisdom.\" — Oprah Winfrey",
        "\"Difficulties strengthen the mind, as labor does the body.\" — Seneca",
        "\"Time is the most valuable thing a man can spend.\" — Theophrastus",
        "\"Only the educated are free.\" — Epictetus",
        "\"Begin at once to live, and count each separate day as a separate life.\" — Seneca"
    )

    private const val PREF_CUSTOM_QUOTES = "pref_custom_quotes_json"
    private var lastQuoteIndex = -1

    fun getQuotes(context: Context): MutableList<String> {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val jsonStr = prefs.getString(PREF_CUSTOM_QUOTES, null)
        if (jsonStr.isNullOrEmpty()) {
            return DEFAULT_QUOTES.toMutableList()
        }
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val q = arr.getString(i).trim()
                if (q.isNotEmpty()) list.add(q)
            }
            if (list.isEmpty()) DEFAULT_QUOTES.toMutableList() else list
        } catch (e: Exception) {
            DEFAULT_QUOTES.toMutableList()
        }
    }

    fun saveQuotes(context: Context, quotes: List<String>) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val arr = JSONArray()
        for (q in quotes) {
            if (q.trim().isNotEmpty()) arr.put(q.trim())
        }
        prefs.edit().putString(PREF_CUSTOM_QUOTES, arr.toString()).apply()
    }

    fun getNextQuote(context: Context): String {
        val quotes = getQuotes(context)
        if (quotes.isEmpty()) return DEFAULT_QUOTES[0]
        if (quotes.size == 1) return quotes[0]
        var nextIdx = Random().nextInt(quotes.size)
        if (nextIdx == lastQuoteIndex) {
            nextIdx = (nextIdx + 1) % quotes.size
        }
        lastQuoteIndex = nextIdx
        return quotes[nextIdx]
    }

    fun getRandomQuote(context: Context): String {
        return getNextQuote(context)
    }

    fun addQuote(context: Context, quote: String) {
        val list = getQuotes(context)
        list.add(0, quote.trim())
        saveQuotes(context, list)
    }

    fun removeQuote(context: Context, index: Int) {
        val list = getQuotes(context)
        if (index in 0 until list.size) {
            list.removeAt(index)
            saveQuotes(context, list)
        }
    }

    fun resetQuotes(context: Context) {
        saveQuotes(context, DEFAULT_QUOTES)
    }
}
