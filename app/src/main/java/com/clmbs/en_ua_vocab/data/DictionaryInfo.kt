package com.clmbs.en_ua_vocab.data

data class DictionaryInfo(
    val id: String,
    val name: String,
    val filePath: String?, // null for built-in
    var isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false
)
