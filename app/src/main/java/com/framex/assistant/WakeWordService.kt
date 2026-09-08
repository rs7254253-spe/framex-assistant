package com.framex.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Runs continuously in the foreground. Listens for the wake word "Rex" — a CUSTOM
 * keyword, not one of Porcupine's free built-in words. You must train it yourself
 * (free) at console.picovoice.ai and drop the downloaded file into
 * app/src/main/assets/rex.ppn — see README.md for exact steps.
 *
 * Loop: wake word heard -> stop wake-word engine -> listen for a command via
 * SpeechRecognizer -> send to backend -> speak the reply -> resume wake-word engine.
 */
class WakeWordService : Service() {

    // Get a free AccessKey at console.picovoice.ai and paste it here.
    private val PICOVOICE_ACCESS_KEY = "REPLACE-WITH-YOUR-PICOVOICE-ACCESS-KEY"

    // Name of the custom keyword file you trained for "Rex" and placed in assets/.
    private val KEYWORD_ASSET_NAME = "rex.ppn"

    private var porcupineManager: PorcupineManager? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val conversationHistory = mutableListOf<Pair<String, String>>()

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification("Listening for \"Rex\"…"))
        setupTts()
        setupPorcupine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                // Resume listening for the wake word once we're done talking.
                resumeWakeWordListening()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                resumeWakeWordListening()
            }
        })
    }

    /** Porcupine needs a real file path, so copy the .ppn out of assets/ once. */
    private fun copyKeywordFileIfNeeded(): String {
        val outFile = File(filesDir, KEYWORD_ASSET_NAME)
        if (!outFile.exists()) {
            assets.open(KEYWORD_ASSET_NAME).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile.absolutePath
    }

    private fun setupPorcupine() {
        try {
            val keywordPath = copyKeywordFileIfNeeded()
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(PICOVOICE_ACCESS_KEY)
                .setKeywordPath(keywordPath)
                .build(applicationContext, wakeWordCallback)
            porcupineManager?.start()
        } catch (e: Exception) {
            // Most common cause: rex.ppn missing from assets/, or wrong AccessKey.
            updateNotification("Setup error: ${e.message}")
        }
    }

    private val wakeWordCallback = PorcupineManagerCallback { _ ->
        // Wake word detected — pause wake-word engine and start listening for a command.
        pauseWakeWordListening()
        updateNotification("Yes? Listening…")
        startCommandRecognition()
    }

    private fun startCommandRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                speechRecognizer?.destroy()
                if (!text.isNullOrBlank()) {
                    handleCommand(text)
                } else {
                    resumeWakeWordListening()
                }
            }

            override fun onError(error: Int) {
                speechRecognizer?.destroy()
                resumeWakeWordListening()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun handleCommand(text: String) {
        updateNotification("Thinking…")
        BackendClient.send(text, conversationHistory) { reply ->
            if (reply == null) {
                speak("Sorry, I couldn't reach the server just now.")
                return@send
            }
            conversationHistory.add("user" to text)
            conversationHistory.add("assistant" to reply.answer)
            // Keep history from growing forever
            while (conversationHistory.size > 20) conversationHistory.removeAt(0)

            val toSpeak = if (reply.correction != null) {
                "${reply.correction}. ${reply.answer}"
            } else {
                reply.answer
            }
            updateNotification("Listening for \"Rex\"…")
            speak(toSpeak)
        }
    }

    private fun speak(text: String) {
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "reply-${System.currentTimeMillis()}")
    }

    private fun pauseWakeWordListening() {
        try { porcupineManager?.stop() } catch (e: Exception) { /* ignore */ }
    }

    private fun resumeWakeWordListening() {
        try { porcupineManager?.start() } catch (e: Exception) { /* ignore */ }
        updateNotification("Listening for \"Rex\"…")
    }

    private fun buildNotification(text: String): Notification {
        val channelId = "framex_assistant_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Rex", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Rex")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        porcupineManager?.stop()
        porcupineManager?.delete()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
