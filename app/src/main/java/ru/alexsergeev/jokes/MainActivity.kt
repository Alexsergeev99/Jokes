package ru.alexsergeev.jokes

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

const val BASE_URL = "https://api.chucknorris.io"
const val JOKE_KEY_PREFS = "JOKE_KEY_PREFS"
const val JOKE_KEY = "JOKE"
const val TIMER_KEY = "TIMER"
const val FIVE_MINUTES: Long = 300000
const val ONE_SECOND: Long = 1000

class MainActivity : Activity() {

    private lateinit var jokeText: TextView
    private lateinit var getNewJokeButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var handler: Handler
    private var joke: String? = null
    private var timerRunnable: Runnable? = null
    private var updatingTime: Long = FIVE_MINUTES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        jokeText = findViewById(R.id.jokeTextView)
        getNewJokeButton = findViewById(R.id.newJokeButton)
        handler = Handler()
        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        joke = savedInstanceState?.getString(JOKE_KEY)

        if (joke == null) {
            joke = sharedPreferences.getString(JOKE_KEY_PREFS, null)
            if (joke == null) {
                fetchJoke()
            }
        }

        joke?.let { jokeText.text = it }
        updatingTime = sharedPreferences.getLong(TIMER_KEY, 300000)
        startTimer()

        getNewJokeButton.setOnClickListener {
            fetchJoke()
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(JOKE_KEY, joke)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        joke = savedInstanceState.getString(JOKE_KEY)
        joke?.let { jokeText.text = it }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (updatingTime > 0) {
                    updatingTime -= ONE_SECOND
                    handler.postDelayed(this, ONE_SECOND)
                } else {
                    fetchJoke()
                    updatingTime = FIVE_MINUTES
                }
            }
        }
        handler.postDelayed(timerRunnable as Runnable, ONE_SECOND)
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        sharedPreferences.edit().putLong(TIMER_KEY, updatingTime).apply()
    }

    private fun fetchJoke() {
        Thread {
            val joke = getJokeFromNetwork()
            runOnUiThread {
                this.joke = joke
                jokeText.text = joke
                sharedPreferences.edit().putString(JOKE_KEY_PREFS, joke).apply()
            }
        }.start()
    }

    private fun getJokeFromNetwork(): String {

        var joke: String = getString(R.string.something_went_wrong)

        try {
            val url = URL("$BASE_URL/jokes/random")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            reader.forEachLine { response.append(it) }
            reader.close()

            val jsonResponse = JSONObject(response.toString())
            joke = jsonResponse.getString("value")

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return joke
    }
}