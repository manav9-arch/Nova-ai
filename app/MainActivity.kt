package com.nova.ai

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private val apiUrl =
        "https://red-mountain-f307.motiharijan123456.workers.dev/chat"

    private lateinit var input: EditText
    private lateinit var chat: TextView
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech

    private val history = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNovaUI()

        tts = TextToSpeech(this, this)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
        }
    }

    private fun createNovaUI() {

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setPadding(24, 24, 24, 24)

        val title = TextView(this)
        title.text = "Nova AI"
        title.textSize = 28f

        status = TextView(this)
        status.text = "● Nova Online"
        status.textSize = 16f

        chat = TextView(this)
        chat.text = "Nova: Hello! Main Nova hoon. 👋\n\n"
        chat.textSize = 17f

        val scroll = ScrollView(this)
        scroll.addView(chat)

        input = EditText(this)
        input.hint = "Nova se baat karo..."

        val send = Button(this)
        send.text = "Send"

        val voice = Button(this)
        voice.text = "🎙️ Voice"

        val buttons = LinearLayout(this)
        buttons.orientation = LinearLayout.HORIZONTAL

        buttons.addView(
            voice,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        buttons.addView(
            send,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        main.addView(title)
        main.addView(status)

        main.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        main.addView(input)
        main.addView(buttons)

        setContentView(main)

        send.setOnClickListener {
            sendMessage()
        }

        voice.setOnClickListener {
            startVoice()
        }
    }

    private fun sendMessage() {

        val message = input.text.toString().trim()

        if (message.isEmpty()) return

        input.setText("")

        chat.append("You: $message\n\n")
        status.text = "● Nova Thinking..."

        thread {

            try {

                val body = JSONObject()

                body.put("message", message)
                body.put("history", history)

                val connection =
                    URL(apiUrl).openConnection()
                        as HttpURLConnection

                connection.requestMethod = "POST"
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                connection.outputStream.use {
                    it.write(body.toString().toByteArray())
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                connection.disconnect()

                val json = JSONObject(response)

                val reply = json.optString(
                    "reply",
                    "Mujhe reply nahi mila."
                )

                runOnUiThread {

                    chat.append(
                        "Nova: $reply\n\n"
                    )

                    status.text =
                        "● Nova Online"

                    speak(reply)
                }

            } catch (e: Exception) {

                runOnUiThread {

                    status.text =
                        "● Connection Error"

                    chat.append(
                        "Nova: Connection problem.\n\n"
                    )
                }
            }
        }
    }

    private fun startVoice() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            Toast.makeText(
                this,
                "Voice recognition available nahi hai.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "hi-IN"
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Nova ko bolo..."
        )

        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 100 &&
            resultCode == RESULT_OK) {

            val results =
                data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )

            val text = results?.firstOrNull()

            if (!text.isNullOrBlank()) {

                input.setText(text)

                sendMessage()
            }
        }
    }

    override fun onInit(result: Int) {

        if (result == TextToSpeech.SUCCESS) {

            tts.language = Locale("hi", "IN")
        }
    }

    private fun speak(text: String) {

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "nova-response"
        )
    }

    override fun onDestroy() {

        tts.stop()
        tts.shutdown()

        super.onDestroy()
    }
}
