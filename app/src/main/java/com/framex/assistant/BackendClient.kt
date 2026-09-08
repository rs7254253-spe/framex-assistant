package com.framex.assistant

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Talks to YOUR Netlify Function (see netlify/functions/chat.js), which holds the
 * real Anthropic API key safely on the server side. The app never sees that key.
 *
 * IMPORTANT: replace BACKEND_URL below with your deployed Netlify Function URL,
 * e.g. "https://your-site-name.netlify.app/.netlify/functions/chat"
 */
object BackendClient {

    private const val BACKEND_URL =  "https://framex-assistant.netlify.app/.netlify/functions/chat"

    private val client = OkHttpClient()

    data class AssistantReply(val correction: String?, val answer: String)

    fun send(
        userText: String,
        history: List<Pair<String, String>>, // (role, content) pairs, oldest first
        onResult: (AssistantReply?) -> Unit
    ) {
        val messages = JSONArray()
        for ((role, content) in history) {
            messages.put(JSONObject().put("role", role).put("content", content))
        }
        messages.put(JSONObject().put("role", "user").put("content", userText))

        val body = JSONObject().put("messages", messages)
        val requestBody = body.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BACKEND_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(null)
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        onResult(null)
                        return
                    }
                    val text = it.body?.string() ?: return onResult(null)
                    try {
                        val json = JSONObject(text)
                        val full = json.optString("reply", "")
                        var correction: String? = null
                        var answer = full
                        val lines = full.split("\n")
                        if (lines.isNotEmpty() && lines[0].trim().lowercase().startsWith("correction:")) {
                            correction = lines[0].trim()
                            answer = lines.drop(1).joinToString("\n").trim()
                        }
                        onResult(AssistantReply(correction, answer))
                    } catch (e: Exception) {
                        onResult(null)
                    }
                }
            }
        })
    }
}
