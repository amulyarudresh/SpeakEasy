package com.example.speakeasy

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = "SpeakEasy"

    // Secure API Key from Secrets.kt
    private val apiKey = Secrets.GEMINI_API_KEY

    // Managers
    private lateinit var historyManager: HistoryManager
    private lateinit var ttsManager: TtsManager
    private lateinit var aiManager: AiManager

    private var rewardedAd: RewardedAd? = null

    lateinit var btnRecord: Button
    lateinit var btnProReport: Button
    lateinit var txtResult: TextView
    lateinit var txtAiSpeech: TextView
    lateinit var volumeMeter: WaveformView
    lateinit var spinnerScenario: Spinner
    lateinit var spinnerTone: Spinner
    lateinit var txtEqEmoji: TextView
    lateinit var txtFillerCount: TextView
    lateinit var btnHistory: Button
    lateinit var btnPremium: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    private var isButtonHeld = false
    private val speechBuffer = StringBuilder()
    private val chatHistory = StringBuilder() // Keep local buffer for context
    
    private var currentScenario = "General Chat"
    private var currentTone = "Friendly"
    private var fillerCount = 0

    // Analysis Metrics
    private var speechStartTime: Long = 0
    private val rmsValues = mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Managers
        historyManager = HistoryManager(this)
        aiManager = AiManager(apiKey)
        ttsManager = TtsManager(this) { success ->
            if (success) {
                // Load history on startup
                val savedHistory = historyManager.loadHistory()
                if (savedHistory.isNotEmpty()) {
                    chatHistory.append(savedHistory)
                    // Trim if too long
                    if (chatHistory.length > 2000) {
                        val trimmed = chatHistory.substring(chatHistory.length - 2000)
                        chatHistory.clear()
                        chatHistory.append("...").append(trimmed)
                    }
                }
                
                val intro = getString(R.string.intro_message)
                // Don't append intro to history if it's already there from previous session? 
                // Actually, let's just speak it.
                ttsManager.speak(intro)
            }
        }

        // 2. Initialize AdMob
        MobileAds.initialize(this) {}
        val mAdView = findViewById<AdView>(R.id.adView)
        mAdView.loadAd(AdRequest.Builder().build())
        loadRewardedAd()

        // 3. Setup Views
        btnRecord = findViewById(R.id.btnRecord)
        btnProReport = findViewById(R.id.btnProReport)
        txtResult = findViewById(R.id.txtResult)
        txtAiSpeech = findViewById(R.id.txtAiSpeech)
        volumeMeter = findViewById(R.id.volumeMeter)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        setupSpeechRecognizer()

        // 4. Setup Listeners
        setupRecordButton()
        setupScenarioSpinner()
        setupToneSpinner()
        
        txtEqEmoji = findViewById(R.id.txtEqEmoji)
        txtFillerCount = findViewById(R.id.txtFillerCount)
        btnHistory = findViewById(R.id.btnHistory)
        btnPremium = findViewById(R.id.btnPremium)

        btnPremium.setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        btnHistory.setOnClickListener {
            showHistoryDialog()
        }

        btnProReport.setOnClickListener {
            showRewardedAd()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRecordButton() {
        btnRecord.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isButtonHeld = true
                    speechBuffer.clear() // Start fresh
                    fillerCount = 0
                    txtFillerCount.text = "0"
                    txtFillerCount.setTextColor(Color.parseColor("#D32F2F")) // Reset color
                    startRecording()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isButtonHeld = false
                    stopRecording()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupScenarioSpinner() {
        spinnerScenario = findViewById(R.id.spinnerScenario)
        ArrayAdapter.createFromResource(
            this,
            R.array.scenarios,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerScenario.adapter = adapter
        }

        spinnerScenario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val selected = parent.getItemAtPosition(pos).toString()
                // Remove emojis for logic
                currentScenario = selected.split(" ")[0] + " " + selected.split(" ")[1]
                if (currentScenario.contains("General")) currentScenario = "General Chat"
                
                // Clear history when switching scenarios for a fresh start
                chatHistory.clear()
                // Also clear file history? Maybe not, user might want to keep it. 
                // But for context window it's cleared.
                
                val intro = when {
                    currentScenario.contains("Job") -> "Hello. I am the Hiring Manager. Please take a seat. Tell me about yourself."
                    currentScenario.contains("Date") -> "Hey! So... I've been looking forward to this. You look great!"
                    currentScenario.contains("Public") -> "Alright, I'm listening. Deliver your opening statement. Project your voice!"
                    else -> getString(R.string.intro_message)
                }
                ttsManager.speak(intro)
                chatHistory.append("AI: $intro\n")
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupToneSpinner() {
        spinnerTone = findViewById(R.id.spinnerTone)
        ArrayAdapter.createFromResource(
            this,
            R.array.tones,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTone.adapter = adapter
        }

        spinnerTone.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val selected = parent.getItemAtPosition(pos).toString()
                // Remove emojis for logic (e.g. "Friendly 🤝" -> "Friendly")
                currentTone = selected.split(" ")[0]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showHistoryDialog() {
        val history = historyManager.loadHistory()
        if (history.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Conversation History")
                .setMessage(history)
                .setPositiveButton("Close", null)
                .setNegativeButton("Clear") { _, _ ->
                    historyManager.clearHistory()
                    chatHistory.clear()
                    Toast.makeText(this, "History Cleared", Toast.LENGTH_SHORT).show()
                }
                .show()
        } else {
            Toast.makeText(this, "No history found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        // TEST AD ID: ca-app-pub-3940256099942544/5224354917
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${adError.message}")
                rewardedAd = null
            }
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
            }
        })
    }

    private fun showRewardedAd() {
        rewardedAd?.let { ad ->
            ad.show(this, OnUserEarnedRewardListener { rewardItem ->
                // Handle the reward.
                Log.d(TAG, "User earned the reward.")
                // Trigger Pro Analysis
                val lastUserSpeech = chatHistory.toString().split("User: ").lastOrNull()?.split("\n")?.firstOrNull() ?: ""
                askTheAI(lastUserSpeech, 0, 0f, isProMode = true, speakResponse = false)
                btnProReport.visibility = android.view.View.GONE
            })
        } ?: run {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            loadRewardedAd() // Try loading again
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
                speechStartTime = System.currentTimeMillis()
                rmsValues.clear()
                volumeMeter.clear()
            }
            override fun onRmsChanged(rmsdB: Float) {
                rmsValues.add(rmsdB)
                volumeMeter.addAmplitude(rmsdB)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { 
                Log.d(TAG, "End of speech detected")
                stopRecording() 
            }
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Error: $error"
                }
                Log.e(TAG, errorMessage)
                
                if (isButtonHeld) {
                    // If error occurs but button is held (e.g. timeout), restart!
                    startRecording()
                } else {
                    isRecording = false
                    btnRecord.text = getString(R.string.tap_to_reply)
                    btnRecord.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.purple_500)
                    
                    // FIX: If we have buffered speech, process it even if the last chunk failed!
                    if (speechBuffer.isNotEmpty()) {
                        Log.d(TAG, "Error occurred but speech buffer has content. Processing...")
                        processFinalSpeech()
                    }
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    val text = matches[0]
                    speechBuffer.append(text).append(" ")
                    
                    if (isButtonHeld) {
                        // If button is still held, the recognizer stopped due to silence/pause.
                        // Restart it immediately to capture the next part.
                        Log.d(TAG, "Recognizer stopped but button held. Restarting...")
                        startRecording()
                    } else {
                        // Button released, process the full speech
                        processFinalSpeech()
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processFinalSpeech() {
        val fullText = speechBuffer.toString().trim()
        if (fullText.isNotEmpty()) {
            val durationSeconds = (System.currentTimeMillis() - speechStartTime) / 1000.0
            val wordCount = fullText.split("\\s+".toRegex()).size
            val wpm = if (durationSeconds > 0) (wordCount / durationSeconds) * 60 else 0.0
            val avgVolume = if (rmsValues.isNotEmpty()) rmsValues.average() else 0.0
            
            // Hackathon Features
            countFillerWords(fullText)
            updateEqEmoji(wpm, avgVolume)
            
            askTheAI(fullText, wpm.toInt(), avgVolume.toFloat(), isProMode = false)
            
            // Show Pro Report button after speaking
            btnProReport.visibility = android.view.View.VISIBLE
        }
    }

    private fun countFillerWords(text: String) {
        val fillers = listOf("um", "uh", "like", "you know", "basically", "actually")
        var count = 0
        val words = text.lowercase().split("\\s+".toRegex())
        
        for (word in words) {
            if (fillers.contains(word)) count++
        }
        
        // Check for phrases like "you know"
        if (text.lowercase().contains("you know")) count++

        fillerCount = count
        txtFillerCount.text = "$fillerCount"
        
        if (fillerCount > 0) {
            // Flash Red if fillers found
            txtFillerCount.setTextColor(Color.RED)
        } else {
            txtFillerCount.setTextColor(Color.parseColor("#388E3C")) // Green
        }
    }

    private fun updateEqEmoji(wpm: Double, avgVolume: Double) {
        val emoji = when {
            avgVolume > 5.0 && wpm > 150 -> "😠" // Aggressive/Excited
            avgVolume < 2.0 && wpm < 100 -> "😔" // Sad/Unsure
            wpm > 160 -> "😰" // Nervous/Fast
            else -> "😌" // Confident/Calm
        }
        txtEqEmoji.text = emoji
    }

    private fun startRecording() {
        if (ttsManager.isSpeaking()) ttsManager.stop()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN") // Listen for Indian English
        // Prevent premature cutoff (Set to 1 hour to effectively remove limit)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000L)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000L)
        
        speechRecognizer?.startListening(intent)
        isRecording = true
        btnRecord.text = "Recording... (Release to Send)"
        btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_light)
        txtResult.text = getString(R.string.thinking)
    }

    private fun stopRecording() {
        speechRecognizer?.stopListening()
        isRecording = false
        btnRecord.text = getString(R.string.tap_to_reply) 
        btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple_500)
    }

    private fun askTheAI(userSpeech: String, wpm: Int, avgVolume: Float, isProMode: Boolean, speakResponse: Boolean = true) {
        txtResult.text = getString(R.string.user_speech, userSpeech)
        chatHistory.append("User: $userSpeech\n")
        historyManager.saveMessage("User: $userSpeech")

        val prompt = aiManager.constructPrompt(
            userSpeech, 
            chatHistory.toString(), 
            currentScenario, 
            currentTone, 
            wpm, 
            avgVolume, 
            isProMode
        )

        lifecycleScope.launch {
            Log.d(TAG, "Sending prompt to Gemini...")
            try {
                val aiReply = aiManager.generateResponse(prompt) ?: getString(R.string.did_not_understand)
                Log.d(TAG, "Gemini response: $aiReply")
                chatHistory.append("AI: $aiReply\n")
                historyManager.saveMessage("AI: $aiReply")
                
                txtAiSpeech.text = aiReply
                if (speakResponse) {
                    ttsManager.speak(aiReply)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Error: ${e.message}", e)
                val errorMsg = "Error: ${e.localizedMessage}"
                txtResult.text = errorMsg
                ttsManager.speak("I encountered an error. Please check the screen.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        speechRecognizer?.destroy()
    }
}