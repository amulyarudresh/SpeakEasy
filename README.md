# SpeakEasy - AI Communication Coach 🇮🇳🗣️

**SpeakEasy** is a hyper-personalized, culturally aware AI Coach designed to help users master public speaking and English communication. Unlike generic tools, SpeakEasy uses a custom **Indian Persona Engine** to build a real connection with users, offering adaptive teaching and emotional feedback.

## 🚀 Key Features

### 🧠 Intelligent AI Coach
-   **Indian Persona:** The AI understands Indian cultural references (Cricket, Chai, Festivals) and greets you with "Namaste!".
-   **Adaptive Teaching:** Explains complex words in **Simple English**. If you don't understand, it simplifies the explanation with a *different* example.
-   **Emotional Voice:** Uses SSML to speak with excitement (!), pauses (...), and interjections ("Wow!", "Hmm...") for a human-like experience.

### 📊 Real-Time Analytics
-   **Waveform Visualizer:** Beautiful, real-time audio visualization while you speak.
-   **Filler Word Police:** Detects "um", "uh", "like" instantly and flashes a warning.
-   **EQ Meter:** Analyzes your tone (Confident 😌, Nervous 😟, Excited 🤩) in real-time.
-   **Pace & Volume:** Tracks your Words Per Minute (WPM) and loudness (dB).

### 🎯 Scenario Mode
Practice for specific real-world situations:
-   **Job Interview:** AI acts as a Hiring Manager.
-   **First Date:** AI acts as a charming date.
-   **Public Speech:** AI acts as a strict critic.
-   **General Chat:** Friendly practice.

### 💰 Business Model (Freemium)
-   **Free Tier:** Basic feedback and conversation.
-   **Ad-Supported:** Watch a Rewarded Video Ad to unlock a detailed "Pro Report" (Grammar Score, Actionable Tips).
-   **Premium Subscription:** "Go Pro" for unlimited reports, no ads, and cloud save.

## 🛠️ Tech Stack

-   **Language:** Kotlin (Android)
-   **AI Model:** Google Gemini Flash 1.5 API
-   **Text-to-Speech:** Android TTS with SSML (Speech Synthesis Markup Language)
-   **Ads:** Google AdMob (Rewarded Video)
-   **UI:** Material Design 3 (XML Layouts)

## 📸 Screenshots

*(Add your screenshots here)*

## 📦 Setup Instructions

1.  Clone the repository:
    ```bash
    git clone https://github.com/amulyarudresh/SpeakEasy.git
    ```
2.  Open in **Android Studio**.
3.  **Important:** You need a Gemini API Key.
    -   Create a file `app/src/main/java/com/example/speakeasy/Secrets.kt`.
    -   Add your key:
        ```kotlin
        package com.example.speakeasy
        object Secrets {
            const val GEMINI_API_KEY = "YOUR_API_KEY_HERE"
        }
        ```
4.  Build and Run on an Emulator or Physical Device.

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
