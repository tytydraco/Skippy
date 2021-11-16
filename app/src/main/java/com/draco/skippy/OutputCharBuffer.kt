package com.draco.skippy

class OutputCharBuffer(private val size: Int) {
    private val buffer = ArrayDeque<Char>()

    fun add(char: Char) {
        if (buffer.size >= size)
            buffer.removeFirst()
        buffer.add(char)
    }

    fun add(chars: CharArray) {
        for (char in chars)
            add(char)
    }

    fun clear() {
        buffer.clear()
    }

    fun get(): String = buffer.joinToString("")
}