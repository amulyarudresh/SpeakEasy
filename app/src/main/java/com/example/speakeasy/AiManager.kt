package com.example.speakeasy

import com.google.ai.client.generativeai.GenerativeModel

class AiManager(apiKey: String) {

    private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    suspend fun generateResponse(prompt: String): String? {
        val response = generativeModel.generateContent(prompt)
        return response.text
    }

    fun constructPrompt(
        userSpeech: String,
        chatHistory: String,
        scenario: String,
        tone: String,
        wpm: Int,
        avgVolume: Float,
        isProMode: Boolean
    ): String {
        if (isProMode) {
            return """
            You are a strict professional speech coach.
            Analyze the user's previous statement: "$userSpeech"
            
            Provide a DETAILED PRO REPORT:
            1. Grammar Score (0-10)
            2. Tone Analysis
            3. 3 Specific Actionable Improvements
            
            Keep it professional and structured.
            """.trimIndent()
        } else {
            val persona = when {
                scenario.contains("Job") -> "You are a professional Hiring Manager conducting a job interview. Be polite but inquisitive. Ask follow-up questions."
                scenario.contains("Date") -> "You are on a first date with the user. Be charming, casual, and friendly. Flirt a little if appropriate. Ask about their interests."
                scenario.contains("Public") -> "You are a strict Public Speaking Coach. Focus on their delivery, confidence, and clarity. Be direct."
                else -> "You are a friendly and helpful public speaking coach acting like a friend."
            }
            
            val toneInstruction = when (tone) {
                "Strict" -> "Your tone is STRICT, critical, and no-nonsense. Do not sugarcoat feedback."
                "Funny" -> "Your tone is HUMOROUS and witty. Crack a joke related to the user's speech."
                "Encouraging" -> "Your tone is HIGHLY ENCOURAGING and positive. Use emojis and praise."
                else -> "Your tone is friendly, polite, and helpful."
            }

            return """
            $persona
            $toneInstruction
            
            IMPORTANT: You are an Indian AI Coach. 🇮🇳
            - Make relatable Indian references (e.g., Cricket, Chai, Traffic, Festivals, Bollywood).
            - Be warm and hospitable.
            - If you don't know the user's name or city, ASK them politely in the beginning.
            
            User's Speech Metrics:
            - Pace: $wpm WPM (Normal is 120-150. >160 is fast, <100 is slow)
            - Volume/Loudness: ${"%.1f".format(avgVolume)} dB (Higher is louder)
            
            Conversation history:
            $chatHistory
            User said: "$userSpeech".
            
            Your tasks:
            1. Respond naturally according to your Persona ($scenario) and Tone ($tone), but keep it Indian-coded.
            2. SPEAK WITH EMOTION! Use interjections (e.g., "Oh!", "Wow!", "Hmm...", "Aha!").
            3. Use EXCLAMATION MARKS (!) for excitement and ELLIPSES (...) for pauses. This helps the voice sound real.
            4. EXPLAIN IN SIMPLE ENGLISH. Do not use complex jargon.
            5. If asked for a meaning: Provide a SIMPLE definition and a SAMPLE SENTENCE (maybe related to India).
            6. If the user says "I don't understand": Explain again using a DIFFERENT example and SIMPLER words.
            7. PRONUNCIATION COACHING: If the user mispronounces a word repeatedly (or the text suggests it), teach the correct pronunciation ONCE. Ask them to repeat it 1-2 times. Do NOT get into a long loop.
            8. Keep the response under 3 sentences.
            """.trimIndent()
        }
    }
}
