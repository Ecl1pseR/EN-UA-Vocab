package com.clmbs.en_ua_vocab.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.LinkedList

class WordRepository(private val context: Context) {
    private val gson = Gson()
    private var words: List<Word> = emptyList()
    
    private val history = LinkedList<Word>()
    private var historyIndex = -1
    private val MAX_HISTORY = 50

    private val PREFS_NAME = "dictionary_prefs"
    private val KEY_ENABLED_PREFIX = "enabled_"
    private val DICT_DIR = "dictionaries"

    init {
        loadWords()
    }

    fun loadWords() {
        val allWords = mutableListOf<Word>()
        val dictionaries = getDictionaries()
        
        for (dict in dictionaries) {
            if (dict.isEnabled) {
                allWords.addAll(loadWordsFromDictionary(dict))
            }
        }
        
        words = allWords
        Log.d("WordRepository", "Total words loaded: ${words.size}")
    }

    private fun loadWordsFromDictionary(dict: DictionaryInfo): List<Word> {
        return try {
            val reader = if (dict.isBuiltIn) {
                InputStreamReader(context.assets.open("words.json"), StandardCharsets.UTF_8)
            } else {
                val file = File(dict.filePath!!)
                if (!file.exists()) return emptyList()
                FileReader(file)
            }
            
            val type = object : TypeToken<List<Word>>() {}.type
            val list: List<Word> = gson.fromJson(reader, type)
            reader.close()
            list
        } catch (e: Exception) {
            Log.e("WordRepository", "Error loading dict ${dict.name}", e)
            emptyList()
        }
    }

    fun getDictionaries(): List<DictionaryInfo> {
        val result = mutableListOf<DictionaryInfo>()
        
        // Built-in
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val builtInEnabled = prefs.getBoolean(KEY_ENABLED_PREFIX + "builtin", true)
        result.add(DictionaryInfo("builtin", "Вбудований словник", null, builtInEnabled, true))
        
        // External
        val dir = File(context.filesDir, DICT_DIR)
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                val id = file.name
                val enabled = prefs.getBoolean(KEY_ENABLED_PREFIX + id, true)
                result.add(DictionaryInfo(id, file.name, file.absolutePath, enabled, false))
            }
        }
        
        return result
    }

    fun importDictionary(uri: Uri, fileName: String): Boolean {
        return try {
            val dir = File(context.filesDir, DICT_DIR)
            if (!dir.exists()) dir.mkdirs()
            
            val destFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            loadWords()
            true
        } catch (e: Exception) {
            Log.e("WordRepository", "Import failed", e)
            false
        }
    }

    fun toggleDictionary(id: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED_PREFIX + id, enabled).apply()
        loadWords()
    }

    fun deleteDictionary(id: String) {
        val dir = File(context.filesDir, DICT_DIR)
        val file = File(dir, id)
        if (file.exists()) {
            file.delete()
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ENABLED_PREFIX + id).apply()
        loadWords()
    }

    fun getRandomWord(): Word? {
        if (words.isEmpty()) loadWords()
        if (words.isEmpty()) return null
        
        val word = words.random()
        addToHistory(word)
        return word
    }

    private fun addToHistory(word: Word) {
        if (historyIndex < history.size - 1) {
            while (history.size > historyIndex + 1) {
                history.removeLast()
            }
        }
        history.add(word)
        if (history.size > MAX_HISTORY) history.removeFirst()
        historyIndex = history.size - 1
    }

    fun getNextWord(): Word? {
        if (words.isEmpty()) return null
        return if (historyIndex < history.size - 1) {
            historyIndex++
            history[historyIndex]
        } else {
            getRandomWord()
        }
    }

    fun getPreviousWord(): Word? {
        if (history.isEmpty()) return getRandomWord()
        if (historyIndex > 0) historyIndex--
        return history[historyIndex]
    }

    fun getCurrentWord(): Word? {
        if (history.isEmpty()) return getRandomWord()
        return history[historyIndex]
    }

    fun getWordCount(): Int = words.size
}
