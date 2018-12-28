/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Location
import kotlin.math.min

/** A general purpose reader for formats like `.proto`.  */
class SyntaxReader(private val data: CharArray, private val location: Location) {
    /** Our cursor within the document. `data[pos]` is the next character to be read.  */
    private var pos: Int = 0
    /** The number of newline characters encountered thus far.  */
    private var line: Int = 0
    /** The index of the most recent newline character.  */
    private var lineStart: Int = 0

    fun exhausted(): Boolean {
        return pos == data.size
    }

    /** Reads a non-whitespace character and returns it.  */
    fun readChar(): Char {
        val result = peekChar()
        pos++
        return result
    }

    /** Reads a non-whitespace character 'c', or throws an exception.  */
    fun require(c: Char) {
        if (readChar() != c) throw unexpected("expected '$c'")
    }

    /**
     * Peeks a non-whitespace character and returns it. The only difference
     * between this and `readChar` is that this doesn't consume the char.
     */
    fun peekChar(): Char {
        skipWhitespace(true)
        if (pos == data.size) throw unexpected("unexpected end of file")
        return data[pos]
    }

    fun peekChar(c: Char): Boolean {
        if (peekChar() == c) {
            pos++
            return true
        } else {
            return false
        }
    }

    /** Push back the most recently read character.  */
    fun pushBack(c: Char) {
        if (data[pos - 1] != c) throw IllegalArgumentException()
        pos--
    }

    /** Reads a quoted or unquoted string and returns it.  */
    fun readString(): String {
        skipWhitespace(true)
        val c = peekChar()
        return if (c == '"' || c == '\'') readQuotedString() else readWord()
    }

    fun readQuotedString(): String {
        var startQuote = readChar()
        if (startQuote != '"' && startQuote != '\'') throw AssertionError()
        val result = StringBuilder()
        while (pos < data.size) {
            var c = data[pos++]
            if (c == startQuote) {
                if (peekChar() == '"' || peekChar() == '\'') {
                    // Adjacent strings are concatenated. Consume new quote and continue reading.
                    startQuote = readChar()
                    continue
                }
                return result.toString()
            }

            if (c == '\\') {
                if (pos == data.size) throw unexpected("unexpected end of file")
                c = data[pos++]
                when (c) {
                    'a' -> c = 0x7.toChar()
                    'b' -> c = '\b'
                    'n' -> c = '\n'
                    'r' -> c = '\r'
                    't' -> c = '\t'
                    'v' -> c = 0xb.toChar()
                    'f' -> c = 0xc.toChar() // TODO: '\f' // 0x0c.toChar()
                    'x', 'X' -> c = readNumericEscape(16, 2)
                    '0', '1', '2', '3', '4', '5', '6', '7' -> {
                        --pos
                        c = readNumericEscape(8, 3)
                    }
                    else -> {
                    }
                }// use char as-is
            }

            result.append(c)
            if (c == '\n') newline()
        }
        throw unexpected("unterminated string")
    }

    private fun readNumericEscape(radix: Int, len: Int): Char {
        var value = -1
        val endPos = min(pos + len, data.size)
        while (pos < endPos) {
            val digit = hexDigit(data[pos])
            if (digit == -1 || digit >= radix) break
            if (value < 0) {
                value = digit
            } else {
                value = value * radix + digit
            }
            pos++
        }
        if (value < 0) throw unexpected("expected a digit after \\x or \\X")
        return value.toChar()
    }

    private fun hexDigit(c: Char): Int {
        return if (c >= '0' && c <= '9')
            c - '0'
        else if (c >= 'a' && c <= 'f')
            c - 'a' + 10
        else if (c >= 'A' && c <= 'F')
            c - 'A' + 10
        else
            -1
    }

    /** Reads a (paren-wrapped), [square-wrapped] or naked symbol name.  */
    fun readName(): String {
        val optionName: String
        val c = peekChar()
        if (c == '(') {
            pos++
            optionName = readWord()
            if (readChar() != ')') throw unexpected("expected ')'")
        } else if (c == '[') {
            pos++
            optionName = readWord()
            if (readChar() != ']') throw unexpected("expected ']'")
        } else {
            optionName = readWord()
        }
        return optionName
    }

    /** Reads a scalar, map, or type name.  */
    fun readDataType(): String {
        val name = readWord()
        return readDataType(name)
    }

    /** Reads a scalar, map, or type name with `name` as a prefix word.  */
    fun readDataType(name: String): String {
        if (name == "map") {
            if (readChar() != '<') throw unexpected("expected '<'")
            val keyType = readDataType()
            if (readChar() != ',') throw unexpected("expected ','")
            val valueType = readDataType()
            if (readChar() != '>') throw unexpected("expected '>'")

            return "map<$keyType, $valueType>"
        } else {
            return name
        }
    }

    /** Reads a non-empty word and returns it.  */
    fun readWord(): String {
        skipWhitespace(true)
        val start = pos
        while (pos < data.size) {
            val c = data[pos]
            if (c >= 'a' && c <= 'z'
                    || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9'
                    || c == '_'
                    || c == '-'
                    || c == '.') {
                pos++
            } else {
                break
            }
        }
        if (start == pos) {
            throw unexpected("expected a word")
        }
        return String(data, start, pos - start)
    }

    /** Reads an integer and returns it.  */
    fun readInt(): Int {
        var tag = readWord()
        try {
            var radix = 10
            if (tag.startsWith("0x") || tag.startsWith("0X")) {
                tag = tag.substring("0x".length)
                radix = 16
            }
            return tag.toInt(radix)
        } catch (e: Exception) {
            throw unexpected("expected an integer but was $tag")
        }
    }

    /**
     * Like [.skipWhitespace], but this returns a string containing all
     * comment text. By convention, comments before a declaration document that
     * declaration.
     */
    fun readDocumentation(): String {
        var result: String? = null
        while (true) {
            skipWhitespace(false)
            if (pos == data.size || data[pos] != '/') {
                return result ?: ""
            }
            val comment = readComment()
            result = if (result == null) comment else result + "\n" + comment
        }
    }

    /** Reads a comment and returns its body.  */
    private fun readComment(): String {
        if (pos == data.size || data[pos] != '/') throw AssertionError()
        pos++
        val commentType = if (pos < data.size) data[pos++] else -1
        if (commentType == '*'.toInt()) {
            val result = StringBuilder()
            var startOfLine = true

            while (pos + 1 < data.size) {
                val c = data[pos]
                if (c == '*' && data[pos + 1] == '/') {
                    pos += 2
                    return result.toString().trim { it <= ' ' }
                }
                if (c == '\n') {
                    result.append('\n')
                    newline()
                    startOfLine = true
                } else if (!startOfLine) {
                    result.append(c)
                } else if (c == '*') {
                    if (data[pos + 1] == ' ') {
                        pos += 1 // Skip a single leading space, if present.
                    }
                    startOfLine = false
                } else if (!c.isWhitespace()) {
                    result.append(c)
                    startOfLine = false
                }
                pos++
            }
            throw unexpected("unterminated comment")
        } else if (commentType == '/'.toInt()) {
            if (pos < data.size && data[pos] == ' ') {
                pos += 1 // Skip a single leading space, if present.
            }
            val start = pos
            while (pos < data.size) {
                val c = data[pos++]
                if (c == '\n') {
                    newline()
                    break
                }
            }
            return String(data, start, pos - 1 - start)
        } else {
            throw unexpected("unexpected '/'")
        }
    }

    fun tryAppendTrailingDocumentation(documentation: String): String {
        // Search for a '/' character ignoring spaces and tabs.
        while (pos < data.size) {
            val c = data[pos]
            if (c == ' ' || c == '\t') {
                pos++
            } else if (c == '/') {
                pos++
                break
            } else {
                // Not a whitespace or comment-starting character. Return original documentation.
                return documentation
            }
        }

        if (pos == data.size || data[pos] != '/' && data[pos] != '*') {
            pos-- // Backtrack to start of comment.
            throw unexpected("expected '//' or '/*'")
        }
        val isStar = data[pos] == '*'
        pos++

        if (pos < data.size && data[pos] == ' ') {
            pos++ // Skip a single leading space, if present.
        }

        val start = pos
        var end: Int

        if (isStar) {
            // Consume star comment until it closes on the same line.
            while (true) {
                if (pos == data.size) {
                    throw unexpected("trailing comment must be closed")
                }
                if (data[pos] == '*' && pos + 1 < data.size && data[pos + 1] == '/') {
                    end = pos - 1 // The character before '*'.
                    pos += 2 // Skip to the character after '/'.
                    break
                }
                pos++
            }
            // Ensure nothing follows a trailing star comment.
            while (pos < data.size) {
                val c = data[pos++]
                if (c == '\n') {
                    newline()
                    break
                }
                if (c != ' ' && c != '\t') {
                    throw unexpected("no syntax may follow trailing comment")
                }
            }
        } else {
            // Consume comment until newline.
            while (true) {
                if (pos == data.size) {
                    end = pos - 1
                    break
                }
                val c = data[pos++]
                if (c == '\n') {
                    newline()
                    end = pos - 2 // Account for stepping past the newline.
                    break
                }
            }
        }

        // Remove trailing whitespace.
        while (end > start && (data[end] == ' ' || data[end] == '\t')) {
            end--
        }

        if (end == start) {
            return documentation
        }

        val trailingDocumentation = String(data, start, end - start + 1)

        return if (documentation.isEmpty())
            trailingDocumentation
        else
            documentation + '\n'.toString() + trailingDocumentation
    }

    /**
     * Skips whitespace characters and optionally comments. When this returns,
     * either `pos == data.length` or a non-whitespace character.
     */
    private fun skipWhitespace(skipComments: Boolean) {
        while (pos < data.size) {
            val c = data[pos]
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                pos++
                if (c == '\n') newline()
            } else if (skipComments && c == '/') {
                readComment()
            } else {
                break
            }
        }
    }

    /** Call this every time a '\n' is encountered.  */
    private fun newline() {
        line++
        lineStart = pos
    }

    fun location(): Location {
        return location.at(line + 1, pos - lineStart + 1)
    }

    fun unexpected(message: String): RuntimeException {
        return unexpected(location(), message)
    }

    fun unexpected(location: Location, message: String): RuntimeException {
        throw IllegalStateException("Syntax error in $location: $message")
    }
}
