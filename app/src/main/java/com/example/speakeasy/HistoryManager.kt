package com.example.speakeasy

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class HistoryManager(private val context: Context) {

    private val fileName = "chat_history.txt"
    private val TAG = "HistoryManager"

    fun saveMessage(text: String) {
        try {
            val outputStreamWriter = OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_APPEND))
            outputStreamWriter.write(text + "\n")
            outputStreamWriter.close()
        } catch (e: Exception) {
            Log.e(TAG, "File write failed: " + e.toString())
        }
    }

    fun loadHistory(): String {
        try {
            val inputStream = context.openFileInput(fileName)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()

                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append(receiveString).append("\n")
                }
                inputStream.close()
                return stringBuilder.toString()
            }
        } catch (e: java.io.FileNotFoundException) {
            // Normal behavior on first run, no history yet.
            return ""
        } catch (e: Exception) {
            Log.e(TAG, "File read failed: " + e.toString())
        }
        return ""
    }

    fun clearHistory() {
        try {
            context.deleteFile(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history: " + e.toString())
        }
    }
}
