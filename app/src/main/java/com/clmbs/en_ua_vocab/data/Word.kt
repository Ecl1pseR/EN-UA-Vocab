package com.clmbs.en_ua_vocab.data

import java.io.Serializable

data class Word(
    val word: String,
    val translation: String,
    val level: String,
    val pos: String
) : Serializable
