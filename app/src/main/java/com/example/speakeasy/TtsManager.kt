package com.example.speakeasy

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context, onInitListener: (Boolean) -> Unit) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val TAG = "TtsManager"
    private val onInitCallback = onInitListener

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS Initialized successfully")
            setupNaturalVoice()
            onInitCallback(true)
        } else {
            Log.e(TAG, "TTS Initialization failed!")
            onInitCallback(false)
        }
    }

    private fun setupNaturalVoice() {
        tts?.let { t ->
            val voices = t.voices
            val indianLocale = Locale("en", "IN")
            
            // Try to find a high quality "Network" voice for India
            val targetVoice = voices?.find { 
                it.locale == indianLocale && it.name.contains("network", ignoreCase = true) 
            } ?: voices?.find { 
                it.locale == indianLocale // Fallback to any Indian voice
            }
            
            if (targetVoice != null) {
                t.voice = targetVoice
                Log.d(TAG, "Voice set to: ${targetVoice.name}")
            } else {
                // Fallback if no Indian voice found
                t.language = indianLocale
            }
            
            // Tuning for conversational tone
            t.setPitch(1.0f) 
            t.setSpeechRate(0.9f) 
        }
    }

    fun speak(text: String) {
        // Clean text for TTS (remove asterisks)
        val cleanText = text.replace("*", "")
        
        // Use SSML for emotion
        val ssmlText = generateSsml(cleanText)
        Log.d(TAG, "Speaking SSML: $ssmlText")
        
        tts?.speak(ssmlText, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun generateSsml(text: String): String {
        val sb = StringBuilder()
        sb.append("<speak>")
        
        // Split by sentences to apply prosody individually
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        
        for (sentence in sentences) {
            when {
                sentence.contains("!") -> {
                    // Excited/Strong emotion -> Higher pitch, slightly faster
                    sb.append("<prosody pitch=\"+10%\" rate=\"1.05\">$sentence</prosody> ")
                }
                sentence.contains("?") -> {
                    // Question -> Slightly higher pitch at end (natural, but let's boost it)
                    sb.append("<prosody pitch=\"+5%\">$sentence</prosody> ")
                }
                sentence.contains("...") -> {
                    // Pause/Thinking -> Slower
                    val parts = sentence.split("...")
                    for ((index, part) in parts.withIndex()) {
                        sb.append(part)
                        if (index < parts.size - 1) {
                            sb.append("<break time=\"400ms\"/>")
                        }
                    }
                    sb.append(" ")
                }
                else -> {
                    // Normal
                    sb.append("$sentence ")
                }
            }
        }
        
        sb.append("</speak>")
        return sb.toString()
    }

    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
    
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }
}
