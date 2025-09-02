// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jewel.ui.component.search

public fun interface SpeedSearchMatcher {
    public fun matches(text: String?): List<IntRange>?

    public companion object
}

public fun SpeedSearchMatcher.Companion.substringMatcher(
    pattern: String,
    ignoreCase: Boolean = true,
): SpeedSearchMatcher = SubstringSpeedSearchMatcher(pattern, ignoreCase)

public fun SpeedSearchMatcher.Companion.patternMatcher(
    pattern: String,
    matchFromBeginning: Boolean = false,
    caseSensitivity: MatchingCaseSensitivity = MatchingCaseSensitivity.NONE,
    hardSeparators: String = "",
): SpeedSearchMatcher =
    PatternSpeedSearchMatcher(
        basePattern = pattern.convertToPattern(matchFromBeginning),
        options = caseSensitivity,
        hardSeparators = hardSeparators,
        containsMatcher = substringMatcher(pattern, caseSensitivity != MatchingCaseSensitivity.ALL),
    )

/**
 * Tells whether a string matches a specific substring. Allows for lowercase substring matching.
 *
 * **Swing equivalent:**
 * [MinusculeMatcherImpl.matchBySubstring](https://github.com/JetBrains/intellij-community/blob/master/platform/util/text-matching/src/com/intellij/psi/codeStyle/MinusculeMatcherImpl.java)
 */
private class SubstringSpeedSearchMatcher(private val pattern: String, private val ignoreCase: Boolean = true) :
    SpeedSearchMatcher {
    override fun matches(text: String?): List<IntRange>? {
        if (pattern.isBlank() || text.isNullOrBlank()) return null
        val matchIndex = text.indexOf(pattern, ignoreCase = ignoreCase)
        return if (matchIndex >= 0) {
            listOf(matchIndex..(matchIndex + pattern.length))
        } else {
            null
        }
    }
}

/**
 * Case sensitivity options for matching.
 *
 * **Swing equivalent:**
 * [NameUtil.MatchingCaseSensitivity](https://github.com/JetBrains/intellij-community/blob/master/platform/util/text-matching/src/com/intellij/psi/codeStyle/NameUtil.java)
 */
public enum class MatchingCaseSensitivity {
    NONE,
    FIRST_LETTER,
    ALL,
}

/**
 * Tells whether a string matches a specific pattern. Allows for lowercase camel-hump matching.
 *
 * **Swing equivalent:**
 * [MinusculeMatcherImpl](https://github.com/JetBrains/intellij-community/blob/master/platform/util/text-matching/src/com/intellij/psi/codeStyle/MinusculeMatcherImpl.java)
 *
 * **Helper functions based on:**
 * [NameUtilCore](https://github.com/JetBrains/intellij-community/blob/master/platform/util/base/src/com/intellij/util/text/NameUtilCore.java)
 */
private class PatternSpeedSearchMatcher(
    basePattern: String,
    private val options: MatchingCaseSensitivity,
    private val hardSeparators: String,
    private val containsMatcher: SpeedSearchMatcher,
) : SpeedSearchMatcher {
    private val pattern: CharArray = basePattern.trimEnd('*', ' ').toCharArray()
    private val isLowerCase: BooleanArray = BooleanArray(basePattern.length)
    private val isUpperCase: BooleanArray = BooleanArray(basePattern.length)
    private val isWordSeparator: BooleanArray = BooleanArray(basePattern.length)
    private val patternUpperCase: CharArray = CharArray(basePattern.length)
    private val patternLowerCase: CharArray = CharArray(basePattern.length)
    private val ignoreCase = options != MatchingCaseSensitivity.ALL

    private val hasHumps: Boolean
    private val hasSeparators: Boolean
    private val hasDots: Boolean
    private val meaningfulCharacters: CharArray
    private val minNameLength: Int

    init {
        val meaningful = StringBuilder()
        for (k in basePattern.indices) {
            val c = basePattern[k]
            isLowerCase[k] = c.isLowerCase()
            isUpperCase[k] = c.isUpperCase()
            isWordSeparator[k] = c.isWordSeparator()
            patternUpperCase[k] = c.uppercaseChar()
            patternLowerCase[k] = c.lowercaseChar()

            if (!isWildcard(k)) {
                meaningful.append(patternLowerCase[k])
                meaningful.append(patternUpperCase[k])
            }
        }
        var start = 0
        while (isWildcard(start)) start++
        hasHumps = hasFlag(start + 1, isUpperCase) && hasFlag(start, isLowerCase)
        hasSeparators = hasFlag(start, isWordSeparator)
        hasDots = (start until pattern.size).any { pattern[it] == '.' }
        meaningfulCharacters = meaningful.toString().toCharArray()
        minNameLength = meaningfulCharacters.size / 2
    }

    override fun matches(text: String?): List<IntRange>? =
        if (text.isNullOrBlank()) {
            null
        } else {
            text.matchingFragments()
        }

    private fun String.matchingFragments(): List<IntRange>? {
        if (length < minNameLength) {
            return null
        }

        if (pattern.size > MAX_CAMEL_HUMP_MATCHING_LENGTH) {
            return containsMatcher.matches(this)
        }

        val length = length
        var patternIndex = 0
        for (i in 0 until length) {
            if (patternIndex >= meaningfulCharacters.size) break
            val c = this[i]
            if (c == meaningfulCharacters[patternIndex] || c == meaningfulCharacters[patternIndex + 1]) {
                patternIndex += 2
            }
        }
        if (patternIndex < minNameLength * 2) {
            return null
        }
        return matchWildcards(0, 0, isAscii())
    }

    /**
     * After a wildcard (* or space), search for the first non-wildcard pattern character in the name starting from
     * nameIndex and try to [matchFragment] for it.
     */
    private fun String.matchWildcards(patternIndex: Int, nameIndex: Int, isAscii: Boolean): List<IntRange>? {
        if (nameIndex < 0) {
            return null
        }
        if (!isWildcard(patternIndex)) {
            if (patternIndex == pattern.size) {
                return null
            }
            return matchFragment(patternIndex, nameIndex, isAscii)
        }

        var currentPatternIndex = patternIndex
        do {
            currentPatternIndex++
        } while (isWildcard(currentPatternIndex))

        if (currentPatternIndex == pattern.size) {
            // Suppressing condition to keep the structure as similar as the Java/Swing files
            @Suppress("ComplexCondition")
            if (
                isTrailingSpacePattern() &&
                    nameIndex != length &&
                    (currentPatternIndex < 2 || !pattern[currentPatternIndex - 2].isUpperCaseOrDigit())
            ) {
                val spaceIndex = indexOf(' ', nameIndex)
                if (spaceIndex >= 0) {
                    return listOf(IntRange.from(spaceIndex, 1))
                }
            }
            return null
        }

        return matchSkippingWords(
            currentPatternIndex,
            findNextPatternCharOccurrence(nameIndex, currentPatternIndex, isAscii),
            true,
            isAscii,
        )
    }

    private fun isTrailingSpacePattern(): Boolean = ' '.isPatternChar(pattern.size - 1)

    /**
     * Enumerates places in name that could be matched by the pattern at patternIndex position and invokes
     * [matchFragment] at those candidate positions
     */
    private fun String.matchSkippingWords(
        patternIndex: Int,
        nameIndex: Int,
        allowSpecialChars: Boolean,
        isAscii: Boolean,
    ): List<IntRange>? {
        var currentNameIndex = nameIndex
        var maxFoundLength = 0
        while (currentNameIndex >= 0) {
            val fragmentLength =
                if (seemsLikeFragmentStart(patternIndex, currentNameIndex)) {
                    maxMatchingFragment(patternIndex, currentNameIndex)
                } else {
                    0
                }

            // match the remaining pattern only if we haven't already seen fragment of the same (or bigger) length
            // because otherwise it means that we already tried to match remaining pattern letters after it with the
            // remaining name and failed
            // but now we have the same remaining pattern letters and even less remaining name letters, and so will fail
            // as well
            if (
                fragmentLength > maxFoundLength ||
                    currentNameIndex + fragmentLength == length && isTrailingSpacePattern()
            ) {
                if (!isMiddleMatch(patternIndex, currentNameIndex)) {
                    maxFoundLength = fragmentLength
                }
                val ranges = matchInsideFragment(patternIndex, currentNameIndex, isAscii, fragmentLength)
                if (ranges != null) {
                    return ranges
                }
            }
            val next = findNextPatternCharOccurrence(currentNameIndex + 1, patternIndex, isAscii)
            currentNameIndex =
                if (allowSpecialChars) next else checkForSpecialChars(currentNameIndex + 1, next, patternIndex)
        }
        return null
    }

    private fun String.findNextPatternCharOccurrence(startAt: Int, patternIndex: Int, isAscii: Boolean): Int =
        if (!'*'.isPatternChar(patternIndex - 1) && !isWordSeparator[patternIndex]) {
            indexOfWordStart(patternIndex, startAt, isAscii)
        } else {
            indexOfIgnoreCase(startAt, pattern[patternIndex], patternIndex, isAscii)
        }

    private fun String.checkForSpecialChars(start: Int, end: Int, patternIndex: Int): Int {
        if (end < 0) return -1

        // pattern humps are allowed to match in words separated by " ()", lowercase characters aren't
        if (!hasSeparators && !hasHumps && substring(start, end).any { hardSeparators.contains(it) }) {
            return -1
        }
        // if the user has typed a dot, don't skip other dots between humps
        // but one pattern dot may match several name dots
        if (hasDots && !'.'.isPatternChar(patternIndex - 1) && substring(start, end).contains('.')) {
            return -1
        }
        return end
    }

    private fun String.seemsLikeFragmentStart(patternIndex: Int, nextOccurrence: Int): Boolean =
        !isUpperCase[patternIndex] ||
            this[nextOccurrence].isUpperCase() ||
            isWordStartingAt(nextOccurrence) ||
            !hasHumps && ignoreCase

    private fun Char.matchPatternChar(patternChar: Char, patternIndex: Int): Boolean =
        patternChar == this ||
            ignoreCase && (patternLowerCase[patternIndex] == this || patternUpperCase[patternIndex] == this)

    private fun String.matchFragment(patternIndex: Int, nameIndex: Int, isAscii: Boolean): List<IntRange>? {
        val fragmentLength = maxMatchingFragment(patternIndex, nameIndex)
        return if (fragmentLength == 0) {
            null
        } else {
            matchInsideFragment(patternIndex, nameIndex, isAscii, fragmentLength)
        }
    }

    private fun String.maxMatchingFragment(patternIndex: Int, nameIndex: Int): Int {
        if (!isFirstCharMatching(nameIndex, patternIndex)) {
            return 0
        }

        var index = 1
        while (nameIndex + index < length && patternIndex + index < pattern.size) {
            val nameChar = this[nameIndex + index]
            if (!nameChar.matchPatternChar(pattern[patternIndex + index], patternIndex + index)) {
                if (nameChar.isSkippingDigitBetweenPatternDigits(patternIndex + index)) {
                    return 0
                }
                break
            }
            index++
        }
        return index
    }

    private fun Char.isSkippingDigitBetweenPatternDigits(patternIndex: Int): Boolean =
        pattern[patternIndex].isDigit() && pattern[patternIndex - 1].isDigit() && isDigit()

    // we've found the longest fragment matching pattern and name
    private fun String.matchInsideFragment(
        patternIndex: Int,
        nameIndex: Int,
        isAscii: Boolean,
        fragmentLength: Int,
    ): List<IntRange>? {
        // exact middle matches have to be at least of length 3, to prevent too many irrelevant matches
        val minFragment = if (isMiddleMatch(patternIndex, nameIndex)) 3 else 1

        val camelHumpRanges = improveCamelHumps(patternIndex, nameIndex, isAscii, fragmentLength, minFragment)
        if (camelHumpRanges != null) {
            return camelHumpRanges
        }

        return findLongestMatchingPrefix(patternIndex, nameIndex, isAscii, fragmentLength, minFragment)
    }

    private fun String.isMiddleMatch(patternIndex: Int, nameIndex: Int): Boolean =
        '*'.isPatternChar(patternIndex - 1) &&
            !isWildcard(patternIndex + 1) &&
            this[nameIndex].isLetterOrDigit() &&
            !isWordStartingAt(nameIndex)

    private fun String.findLongestMatchingPrefix(
        patternIndex: Int,
        nameIndex: Int,
        isAscii: Boolean,
        fragmentLength: Int,
        minFragment: Int,
    ): List<IntRange>? {
        if (patternIndex + fragmentLength >= pattern.size) {
            return listOf(IntRange.from(nameIndex, fragmentLength))
        }

        // try to match the remainder of pattern with the remainder of name
        // it may not succeed with the longest matching fragment, then try shorter matches
        var length = fragmentLength
        while (length >= minFragment || (length > 0 && isWildcard(patternIndex + length))) {
            val ranges: List<IntRange>? =
                if (isWildcard(patternIndex + length)) {
                    matchWildcards(patternIndex + length, nameIndex + length, isAscii)
                } else {
                    var nextOccurrence =
                        findNextPatternCharOccurrence(nameIndex + length + 1, patternIndex + length, isAscii)
                    nextOccurrence = checkForSpecialChars(nameIndex + length, nextOccurrence, patternIndex + length)
                    if (nextOccurrence >= 0) {
                        matchSkippingWords(patternIndex + length, nextOccurrence, false, isAscii)
                    } else {
                        null
                    }
                }
            if (ranges != null) {
                return prependRange(ranges, nameIndex, length)
            }
            length--
        }
        return null
    }

    /**
     * When pattern is "CU" and the name is "CurrentUser", we already have a prefix "Cu" that matches, but we try to
     * find uppercase "U" later in name for better matching degree
     */
    private fun String.improveCamelHumps(
        patternIndex: Int,
        nameIndex: Int,
        isAscii: Boolean,
        maxFragment: Int,
        minFragment: Int,
    ): List<IntRange>? {
        for (i in minFragment until maxFragment) {
            if (isUppercasePatternVsLowercaseNameChar(patternIndex + i, nameIndex + i)) {
                val ranges = findUppercaseMatchFurther(patternIndex + i, nameIndex + i, isAscii)
                if (ranges != null) {
                    return prependRange(ranges, nameIndex, i)
                }
            }
        }
        return null
    }

    private fun String.isUppercasePatternVsLowercaseNameChar(patternIndex: Int, nameIndex: Int): Boolean =
        isUpperCase[patternIndex] && pattern[patternIndex] != this[nameIndex]

    private fun String.findUppercaseMatchFurther(patternIndex: Int, nameIndex: Int, isAscii: Boolean): List<IntRange>? {
        val nextWordStart = indexOfWordStart(patternIndex, nameIndex, isAscii)
        return matchWildcards(patternIndex, nextWordStart, isAscii)
    }

    private fun String.isFirstCharMatching(nameIndex: Int, patternIndex: Int): Boolean {
        if (nameIndex >= length) return false

        val patternChar = pattern[patternIndex]
        if (!this[nameIndex].matchPatternChar(patternChar, patternIndex)) return false

        // Suppressing condition to keep the structure as similar as the Java/Swing files
        @Suppress("ComplexCondition")
        if (
            options == MatchingCaseSensitivity.FIRST_LETTER &&
                (patternIndex == 0 || patternIndex == 1 && isWildcard(0)) &&
                patternChar.hasCase() &&
                patternChar.isUpperCase() != this[0].isUpperCase()
        ) {
            return false
        }
        return true
    }

    private fun isWildcard(patternIndex: Int): Boolean {
        if (patternIndex >= 0 && patternIndex < pattern.size) {
            val currentChar = pattern[patternIndex]
            return currentChar == ' ' || currentChar == '*'
        }
        return false
    }

    private fun Char.isPatternChar(patternIndex: Int): Boolean =
        (patternIndex >= 0) && (patternIndex < pattern.size) && (pattern[patternIndex] == this)

    private fun String.indexOfWordStart(patternIndex: Int, startFrom: Int, isAscii: Boolean): Int {
        val p = pattern[patternIndex]

        // Suppressing condition to keep the structure as similar as the Java/Swing files
        @Suppress("ComplexCondition")
        if (
            startFrom >= length ||
                hasHumps && isLowerCase[patternIndex] && !(patternIndex > 0 && isWordSeparator[patternIndex - 1])
        ) {
            return -1
        }
        var fromIndex = startFrom
        val isSpecialSymbol = !p.isLetterOrDigit()
        while (true) {
            fromIndex = indexOfIgnoreCase(fromIndex, p, patternIndex, isAscii)
            if (fromIndex < 0) return -1

            if (isSpecialSymbol || isWordStartingAt(fromIndex)) return fromIndex

            fromIndex++
        }
    }

    private fun String.indexOfIgnoreCase(fromIndex: Int, p: Char, patternIndex: Int, isAscii: Boolean): Int {
        if (isAscii && p.code < 128) {
            val pUpper = patternUpperCase[patternIndex]
            val pLower = patternLowerCase[patternIndex]
            for (i in fromIndex until length) {
                val c = this[i]
                if (c == pUpper || c == pLower) {
                    return i
                }
            }
            return -1
        }
        return indexOf(p, fromIndex, ignoreCase = true)
    }

    private fun hasFlag(start: Int, flags: BooleanArray): Boolean {
        for (i in start until pattern.size) {
            if (flags[i]) {
                return true
            }
        }
        return false
    }

    private fun prependRange(ranges: List<IntRange>, from: Int, length: Int): List<IntRange> {
        val head = ranges.firstOrNull()
        return if (head != null && head.first == from + length) {
            val tail = ranges.drop(1)
            tail + listOf(IntRange(from, head.last))
        } else {
            listOf(IntRange.from(from, length)) + ranges
        }
    }
}

private fun IntRange.Companion.from(startOffset: Int, length: Int) = startOffset..startOffset + length

private fun Char.isUpperCaseOrDigit(): Boolean = isUpperCase() || isDigit()

private fun Char.hasCase(): Boolean = isUpperCase() || isLowerCase()

private fun Char.isWordSeparator(): Boolean =
    isWhitespace() || this == '_' || this == '-' || this == ':' || this == '+' || this == '.'

/**
 * Detects if this is a new word start.
 *
 * **Swing equivalent:**:
 * [NameUtilCore.isWordStart](https://github.com/JetBrains/intellij-community/blob/master/platform/util/base/src/com/intellij/util/text/NameUtilCore.java)
 */
private fun String.isWordStartingAt(index: Int): Boolean {
    val cur = this[index].code
    val prev = if (index > 0) this[index - 1].code else -1
    if (cur.toChar().isUpperCase()) {
        if (prev.toChar().isUpperCase()) {
            // check that we're not in the middle of an all-caps word
            val nextPos = index + 1
            return nextPos < length && this[nextPos].isLowerCase()
        }
        return true
    }
    if (cur.toChar().isDigit()) {
        return true
    }
    if (!cur.toChar().isLetter()) {
        return false
    }
    if (Character.isIdeographic(cur)) {
        // Consider every ideograph as a separate word
        return true
    }
    return index == 0 || !prev.toChar().isLetterOrDigit() || isHardCodedWordStart(index) || cur.isKanaBreakFrom(prev)
}

/**
 * Word detection methods region.
 *
 * **Swing equivalent:**:
 * [NameUtilCore.isHardCodedWordStart](https://github.com/JetBrains/intellij-community/blob/master/platform/util/base/src/com/intellij/util/text/NameUtilCore.java)
 */
private fun String.isHardCodedWordStart(i: Int): Boolean =
    this[i] == 'l' && i < length - 1 && this[i + 1] == 'n' && (length == i + 2 || isWordStartingAt(i + 2))

/**
 * Gets the next word after a given index.
 *
 * **Swing equivalent:**:
 * [MinusculeMatcherImpl.nextWord](https://github.com/JetBrains/intellij-community/blob/master/platform/util/text-matching/src/com/intellij/psi/codeStyle/MinusculeMatcherImpl.java)
 */
private fun String.nextWordAfter(start: Int): Int {
    if (start < length && this[start].isDigit()) {
        return start + 1 // treat each digit as a separate hump
    }
    return nextWordAfter(start)
}

/**
 * Check if the string is composed by only ascii characters.
 *
 * **Swing equivalent:**:
 * [AsciiUtils.isAscii](https://github.com/JetBrains/intellij-community/blob/master/platform/util/text-matching/src/com/intellij/psi/codeStyle/AsciiUtils.java)
 */
private fun String.isAscii(): Boolean = all { it.code < 128 }

/**
 * **Swing equivalent:**:
 * - * [NameUtilCore.KANA_START](https://github.com/JetBrains/intellij-community/blob/master/platform/util/base/src/com/intellij/util/text/NameUtilCore.java)
 * - * [NameUtilCore.KANA_END](https://github.com/JetBrains/intellij-community/blob/master/platform/util/base/src/com/intellij/util/text/NameUtilCore.java)
 * - * [NameUtilCore.KANA2_START](https://github.com/JetBrains/intellij-community/blob/master/platform/util/base/src/com/intellij/util/text/NameUtilCore.java)
 * - * [NameUtilCore.KANA2_END](https://github.com/JetBrains/intellij-community/blob/master/platform/util/base/src/com/intellij/util/text/NameUtilCore.java)
 */
private val KANA_RANGE = 0x3040..0x3358
private val KANA2_RANGE = 0xFF66..0xFF9D

/**
 * **Swing equivalent:**:
 * [NameUtilCore.maybeKana](https://github.com/JetBrains/intellij-community/blob/master/platform/util/base/src/com/intellij/util/text/NameUtilCore.java)
 */
private fun Int.maybeKana(): Boolean = this in KANA_RANGE || this in KANA2_RANGE

/**
 * **Swing equivalent:**:
 * [NameUtilCore.isKanaBreak](https://github.com/JetBrains/intellij-community/blob/master/platform/util/base/src/com/intellij/util/text/NameUtilCore.java)
 */
private fun Int.isKanaBreakFrom(prev: Int): Boolean {
    if (!maybeKana() && !prev.maybeKana()) return false
    val curScript = Character.UnicodeScript.of(this)
    val prevScript = Character.UnicodeScript.of(prev)
    if (prevScript == curScript) return false
    return (curScript == Character.UnicodeScript.KATAKANA ||
        curScript == Character.UnicodeScript.HIRAGANA ||
        prevScript == Character.UnicodeScript.KATAKANA ||
        prevScript == Character.UnicodeScript.HIRAGANA) &&
        prevScript != Character.UnicodeScript.COMMON &&
        curScript != Character.UnicodeScript.COMMON
}

/**
 * Split the input into words based on case changes, digits, and special characters, and join them with the wildcard
 * ('*') character.
 *
 * **Swing equivalent:**:
 * [SpeedSearchComparator.obtainMatcher](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/ui/SpeedSearchComparator.java)
 */
private fun String.convertToPattern(matchFromBeginning: Boolean): String {
    if (isBlank()) return this

    val buildPattern =
        buildList {
                var index = 0

                while (index < length) {
                    val wordStart = index
                    var upperCaseCount = 0
                    var lowerCaseCount = 0
                    var digitCount = 0
                    var specialCount = 0

                    @Suppress("LoopWithTooManyJumpStatements")
                    while (index < length) {
                        val c = this@convertToPattern[index]
                        when {
                            c.isDigit() -> {
                                if (upperCaseCount > 0 || lowerCaseCount > 0 || specialCount > 0) break
                                digitCount++
                            }
                            c.isUpperCase() -> {
                                if (lowerCaseCount > 0 || digitCount > 0 || specialCount > 0) break
                                if (
                                    upperCaseCount > 1 &&
                                        index + 1 < length &&
                                        this@convertToPattern[index + 1].isLowerCase()
                                ) {
                                    index--
                                    break
                                }
                                upperCaseCount++
                            }
                            c.isLowerCase() -> {
                                if (digitCount > 0 || specialCount > 0) break
                                lowerCaseCount++
                            }
                            else -> {
                                if (upperCaseCount > 0 || lowerCaseCount > 0 || digitCount > 0) break
                                specialCount++
                            }
                        }
                        index++
                    }

                    val word = substring(wordStart, index)
                    if (word.isNotBlank()) {
                        add(word)
                    }
                }
            }
            .joinToString("*")

    return if (!matchFromBeginning && !buildPattern.startsWith("*")) {
        "*$buildPattern"
    } else {
        buildPattern
    }
}

/**
 * Camel-hump matching is >O(n), so for larger prefixes we fall back to simpler matching to avoid pauses
 *
 * **Swing equivalent:**:
 * [MinusculeMatcherImpl.MAX_CAMEL_HUMP_MATCHING_LENGTH](https://github.com/JetBrains/intellij-community/blob/master/platform/util/text-matching/src/com/intellij/psi/codeStyle/MinusculeMatcherImpl.java)
 */
private const val MAX_CAMEL_HUMP_MATCHING_LENGTH = 100
