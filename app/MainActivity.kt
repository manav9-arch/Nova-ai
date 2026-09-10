package com.nova.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Tumhara existing Cloudflare Nova backend
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

        requestMicrophonePermission()
    }

    private fun createNovaUI() {

        val main = LinearLayout(this)

        main.orientation = LinearLayout.VERTICAL
        main.setPadding(24, 24, 24, 24)

        val title = TextView(this)

        title.text = "Nova AI"
        title.textSize = 28f
        title.setPadding(0, 0, 0, 8)

        status = TextView(this)

        status.text = "● Nova Online"
        status.textSize = 15f
        status.setPadding(0, 0, 0, 16)

        chat = TextView(this)

        chat.text =
            "Nova: Hello! Main Nova hoon. 👋\n\n"

        chat.textSize = 17f

        val scroll = ScrollView(this)

        scroll.addView(chat)

        input = EditText(this)

        input.hint = "Nova se baat karo..."
        input.singleLine = true

        val buttons = LinearLayout(this)

        buttons.orientation = LinearLayout.HORIZONTAL

        val voiceButton = Button(this)

        voiceButton.text = "🎙️ Voice"

        val sendButton = Button(this)

        sendButton.text = "Send"

        buttons.addView(
            voiceButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        buttons.addView(
            sendButton,
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

        sendButton.setOnClickListener {
            sendMessage()
        }

        voiceButton.setOnClickListener {
            startVoice()
        }
    }

    private fun sendMessage() {

        val message = input.text.toString().trim()

        if (message.isEmpty()) {
            return
        }

        input.setText("")

        addMessage("You", message)

        status.text = "● Nova Thinking..."

        thread {

            try {

                val requestBody = JSONObject()

                requestBody.put(
                    "message",
                    message
                )

                requestBody.put(
                    "history",
                    history
                )

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

                connection.outputStream.use { output ->

                    output.write(
                        requestBody
                            .toString()
                            .toByteArray()
                    )
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                val json =
                    JSONObject(response)

                val reply =
                    json.optString(
                        "reply",
                        "Mujhe reply nahi mila."
                    )

                runOnUiThread {

                    addMessage(
                        "Nova",
                        reply
                    )

                    status.text =
                        "● Nova Online"

                    speak(reply)
                }

                connection.disconnect()

            } catch (error: Exception) {

                runOnUiThread {

                    status.text =
                        "● Connection Error"

                    addMessage(
                        "Nova",
                        "Connection problem. Dobara try karo."
                    )
                }
            }
        }
    }

    private fun addMessage(
        sender: String,
        message: String
    ) {

        chat.append(
            "$sender: $message\n\n"
        )
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

        val voiceIntent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            )

        voiceIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        voiceIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "hi-IN"
        )

        voiceIntent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Nova ko bolo..."
        )

        startActivityForResult(
            voiceIntent,
            VOICE_REQUEST
        )
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

        if (
            requestCode == VOICE_REQUEST &&
            resultCode == RESULT_OK
        ) {

            val results =
                data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )

            val spokenText =
                results?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {

                input.setText(spokenText)

                sendMessage()
            }
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

    private fun requestMicrophonePermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ),
                MICROPHONE_PERMISSION
            )
        }
    }

    override fun onInit(result: Int) {

        if (
            result ==
            TextToSpeech.SUCCESS
        ) {

            tts.language =
                Locale("hi", "IN")
        }
    }

    override fun onDestroy() {

        tts.stop()

        tts.shutdown()

        super.onDestroy()
    }

    companion object {

        private const val VOICE_REQUEST = 100

        private const val MICROPHONE_PERMISSION = 101
    }
}
