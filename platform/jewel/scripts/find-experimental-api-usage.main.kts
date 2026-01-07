#!/usr/bin/env kotlin

@file:Import("utils.main.kts")
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
@file:Suppress("RAW_RUN_BLOCKING")

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

// Configuration object
private object Config {
    const val ANNOTATION_NAME = "@ExperimentalJewelApi"
    const val MONTHS_THRESHOLD = 6L
    const val KOTLIN_FILE_EXTENSION = ".kt"

    // Regex patterns for Kotlin declarations with @ExperimentalJewelApi
    val ANNOTATION_AFTER_PATTERN = """(?:\s*(?:@\S+(?:\([^)]*\))?\s*|//[^\n]*\n\s*)*)*"""
    val DECLARATION_PATTERNS = listOf(
        // Classes and interfaces
        """$ANNOTATION_NAME$ANNOTATION_AFTER_PATTERN(?:public\s+|private\s+|internal\s+|protected\s+)?(?:open\s+|abstract\s+|sealed\s+|data\s+|value\s+|inline\s+)*(?:class|interface|object|enum class)\s+(\w+)""".toRegex(),
        // Functions
        """$ANNOTATION_NAME$ANNOTATION_AFTER_PATTERN(?:public\s+|private\s+|internal\s+|protected\s+)?(?:suspend\s+|inline\s+|infix\s+|operator\s+|tailrec\s+)*fun\s+(?:<[^>]+>\s+)?(\w+)""".toRegex(),
        // Properties
        """$ANNOTATION_NAME$ANNOTATION_AFTER_PATTERN(?:public\s+|private\s+|internal\s+|protected\s+)?(?:const\s+)?(?:val|var)\s+(\w+)""".toRegex(),
        // Typealiases
        """$ANNOTATION_NAME$ANNOTATION_AFTER_PATTERN(?:public\s+|private\s+|internal\s+|protected\s+)?typealias\s+(\w+)""".toRegex()
    )

    const val GIT_LOG_DATE_FORMAT = "yyyy-MM-dd"
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(GIT_LOG_DATE_FORMAT)

    const val DEFAULT_OUTPUT_FILE = "experimental-api-report.csv"
}

// Data classes
private enum class DeclarationKind {
    CLASS, INTERFACE, OBJECT, FUNCTION, PROPERTY, TYPEALIAS
}

private data class DeclarationInfo(
    val name: String,
    val kind: DeclarationKind,
    val file: File,
    val lineStart: Int,
    val lineEnd: Int,
    val fullText: String,
    val signatureHash: String
)

private data class CommitInfo(
    val hash: String,
    val date: LocalDate,
    val filePath: String
)

private data class HistoricalDeclaration(
    val commit: String,
    val date: LocalDate,
    val exists: Boolean,
    val hasAnnotation: Boolean,
    val fullText: String?,
    val contentHash: String?
)

private data class AnnotationLifecycle(
    val declarationName: String,
    val file: File,
    val lineNumber: Int,
    val lastModifiedCommit: String,
    val lastModifiedDate: LocalDate,
    val currentlyAnnotated: Boolean
) {
    val ageInMonths: Long
        get() = ChronoUnit.MONTHS.between(lastModifiedDate, LocalDate.now())

    val ageInDays: Long
        get() = ChronoUnit.DAYS.between(lastModifiedDate, LocalDate.now())

    fun getStatus(thresholdMonths: Int): String {
        val warningThreshold = (thresholdMonths * 0.75).toLong()
        return when {
            ageInMonths >= thresholdMonths -> "❌"
            ageInMonths >= warningThreshold -> "⚠️"
            else -> "✅"
        }
    }
}

private data class ScanResult(
    val lifecycles: List<AnnotationLifecycle>,
    val totalFilesScanned: Int,
    val filesWithMatches: Int
)

// Command class
private class FindExperimentalApiCommand : SuspendingCliktCommand(name = "find-experimental-api-usage") {
    private val output by option("--output", "-o", help = "Output CSV file path")
        .default(Config.DEFAULT_OUTPUT_FILE)

    private val verbose by option("--verbose", "-v", help = "Show detailed progress information")
        .flag(default = false)

    private val errorMonthsThresholdStr by option("--error-months-threshold", help = "Age threshold in months for error status (default: ${Config.MONTHS_THRESHOLD})")
        .default(Config.MONTHS_THRESHOLD.toString())

    private val folderFilter by option("--folder", "-f", help = "Filter to only scan specific folder (relative to Jewel root)")

    private val errorMonthsThreshold: Int
        get() = errorMonthsThresholdStr.toIntOrNull() ?: Config.MONTHS_THRESHOLD.toInt()

    // Cache for file contents at specific commits
    private val commitFileCache = mutableMapOf<Pair<String, String>, String?>()

    override fun help(context: Context): String = """
        Find Kotlin declarations in platform/jewel annotated with @ExperimentalJewelApi
        that have had the annotation for more than a specified time period.

        This script scans the git history to find when each annotated declaration
        was last modified (including signature, body, or annotation changes).

        Example:
          ./find-experimental-api-usage.main.kts
          ./find-experimental-api-usage.main.kts --output report.csv --error-months-threshold 12
          ./find-experimental-api-usage.main.kts --verbose
    """.trimIndent()

    override suspend fun run() {
        // Validation chain
        val root = validateJewelRoot()
        val gitRoot = validateGitRepo(root)

        println()
        printlnSuccess("Configuration:")
        println("  Annotation: ${Config.ANNOTATION_NAME}")
        println("  Error threshold: $errorMonthsThreshold months (since last modified)")
        println("  Output file: $output")
        if (folderFilter != null) {
            println("  Folder filter: $folderFilter")
        }
        println()

        // Scan for annotated declarations with full lifecycle tracking
        val result = scanForAnnotatedDeclarations(root, gitRoot)

        // Export to CSV (all declarations with status indicators)
        exportToCsv(result.lifecycles, File(output))

        // Report summary
        reportSummary(result)
    }

    private fun validateJewelRoot(): File {
        print("⏳ Locating Jewel root...")
        val root = findJewelRoot()
            ?: exitWithError("Could not find Jewel root directory. Run from inside Jewel.")
        println(" DONE: ${root.absolutePath}")
        return root
    }

    private suspend fun validateGitRepo(root: File): File {
        print("⏳ Checking git repository...")
        if (!isDirectoryGitRepo(root)) {
            println()
            exitWithError("Directory is not a git repository: ${root.absolutePath}")
        }

        val gitRootResult = runCommand(
            command = "git rev-parse --show-toplevel",
            workingDir = root,
            timeoutAmount = 10.seconds,
            exitOnError = false
        )

        if (!gitRootResult.isSuccess) {
            println()
            exitWithError("Failed to find git root directory")
        }

        val gitRoot = File(gitRootResult.output.trim())
        println(" DONE (Git root: ${gitRoot.absolutePath})")
        return gitRoot
    }

    private suspend fun scanForAnnotatedDeclarations(root: File, gitRoot: File): ScanResult {
        print("⏳ Finding Kotlin files...")
        val kotlinFiles = findKotlinFiles(root)
        println(" DONE (${kotlinFiles.size} files)")

        print("⏳ Scanning for ${Config.ANNOTATION_NAME} annotations...")
        val lifecycles = mutableListOf<AnnotationLifecycle>()
        var filesWithMatches = 0

        for ((index, file) in kotlinFiles.withIndex()) {
            if (verbose) {
                println()
                println("⏳ Processing file ${index + 1}/${kotlinFiles.size}: ${file.relativeTo(root).path}")
            }

            val fileLifecycles = processFile(file, root, gitRoot)
            if (fileLifecycles.isNotEmpty()) {
                lifecycles.addAll(fileLifecycles)
                filesWithMatches++

                if (verbose) {
                    println("   Found ${fileLifecycles.size} declarations")
                }
            }
        }

        if (!verbose) {
            println(" DONE")
        }

        println("   Found ${lifecycles.size} declarations in $filesWithMatches files")

        return ScanResult(
            lifecycles = lifecycles,
            totalFilesScanned = kotlinFiles.size,
            filesWithMatches = filesWithMatches
        )
    }

    private fun findKotlinFiles(root: File): List<File> {
        val excludedDirs = parseGitignoreDirectories(root)
        val files = mutableListOf<File>()

        val startDir = if (folderFilter != null) {
            val filtered = root.resolve(folderFilter!!)
            if (!filtered.exists() || !filtered.isDirectory) {
                exitWithError("Folder filter path does not exist or is not a directory: ${filtered.absolutePath}")
            }
            filtered
        } else {
            root
        }

        fun walkDirectory(dir: File) {
            if (dir.name in excludedDirs) return

            dir.listFiles()?.forEach { file ->
                when {
                    file.isDirectory -> walkDirectory(file)
                    file.isFile && file.extension == "kt" -> files.add(file)
                }
            }
        }

        walkDirectory(startDir)
        return files
    }

    private fun parseGitignoreDirectories(root: File): Set<String> {
        val gitignoreFile = root.resolve(".gitignore")
        if (!gitignoreFile.exists()) {
            return setOf(".git", "build", ".gradle", "out", ".idea")
        }

        val excludedDirs = mutableSetOf(".git")

        gitignoreFile.useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    return@forEach
                }

                when {
                    trimmed.endsWith("/") -> {
                        val dirName = trimmed.removeSuffix("/").removePrefix("/")
                        if (!dirName.contains("*") && dirName.isNotEmpty()) {
                            excludedDirs.add(dirName)
                        }
                    }
                    trimmed in listOf(".gradle", ".idea", "out", ".kotlin", ".intellijPlatform") -> {
                        excludedDirs.add(trimmed.removePrefix("/"))
                    }
                }
            }
        }

        return excludedDirs
    }

    private suspend fun processFile(file: File, root: File, gitRoot: File): List<AnnotationLifecycle> {
        // Phase 1: Extract current declarations
        val declarations = extractCurrentDeclarations(file)

        if (declarations.isEmpty()) {
            return emptyList()
        }

        // Phase 2: Get commit history for this file
        val commits = getCommitHistory(file, gitRoot)

        if (commits.isEmpty()) {
            if (verbose) {
                printlnWarn("   No commit history found for ${file.name}")
            }
            return emptyList()
        }

        // Phase 3 & 4: Track each declaration through history
        val lifecycles = mutableListOf<AnnotationLifecycle>()

        for (declaration in declarations) {
            if (verbose) {
                println("   Tracking: ${declaration.name}")
            }

            val history = trackDeclarationHistory(declaration, commits, gitRoot)
            val lifecycle = extractLifecycleDates(declaration, history)

            if (lifecycle != null) {
                lifecycles.add(lifecycle)

                if (verbose) {
                    println("     Last modified: ${lifecycle.lastModifiedDate} (${lifecycle.lastModifiedCommit.take(8)})")
                }
            } else if (verbose) {
                printlnWarn("     Could not determine lifecycle")
            }
        }

        return lifecycles
    }

    private fun extractCurrentDeclarations(file: File): List<DeclarationInfo> {
        val content = file.readText()
        val declarations = mutableListOf<DeclarationInfo>()

        for (pattern in Config.DECLARATION_PATTERNS) {
            pattern.findAll(content).forEach { match ->
                val declarationName = match.groupValues[1]

                // Find the actual start (including annotations before @ExperimentalJewelApi)
                val matchStart = match.range.first
                val actualStart = findDeclarationStart(content, matchStart)
                val lineStart = findLineNumber(content, actualStart)
                val lineEnd = findDeclarationEnd(content, matchStart)

                val fullText = content.lines().subList(lineStart - 1, lineEnd).joinToString("\n")
                val kind = determineKind(fullText)

                declarations.add(DeclarationInfo(
                    name = declarationName,
                    kind = kind,
                    file = file,
                    lineStart = lineStart,
                    lineEnd = lineEnd,
                    fullText = fullText,
                    signatureHash = computeSignatureHash(fullText)
                ))
            }
        }

        return declarations
    }

    private fun findDeclarationStart(content: String, matchPosition: Int): Int {
        // Scan backwards to find any annotations or KDoc before @ExperimentalJewelApi
        var position = matchPosition

        // Move back through whitespace and find the start of the previous line
        while (position > 0 && content[position - 1] in listOf(' ', '\t')) {
            position--
        }

        // Now scan backwards line by line to find annotations or KDoc
        val lines = content.substring(0, position).split('\n')
        var startLine = lines.size - 1

        // Look backwards for annotations or KDoc
        while (startLine > 0) {
            val line = lines[startLine - 1].trim()
            if (line.startsWith("@") || line.startsWith("/**") || line.startsWith("*") || line.startsWith("*/")) {
                startLine--
            } else if (line.isEmpty()) {
                // Empty line, keep going to check for KDoc
                val prevLine = if (startLine > 1) lines[startLine - 2].trim() else ""
                if (prevLine.startsWith("@") || prevLine.contains("*/")) {
                    startLine--
                } else {
                    break
                }
            } else {
                // Non-annotation, non-comment line - stop here
                break
            }
        }

        // Convert back to character position
        return lines.take(startLine).joinToString("\n").length + (if (startLine > 0) 1 else 0)
    }

    private fun findLineNumber(content: String, position: Int): Int {
        return content.substring(0, position).count { it == '\n' } + 1
    }

    private fun findDeclarationEnd(content: String, startPosition: Int): Int {
        val lines = content.lines()
        val startLine = findLineNumber(content, startPosition)

        // For properties and typealiases without braces, just return the same line or next line
        val restOfFile = content.substring(startPosition)

        var braceCount = 0
        var inString = false
        var inChar = false
        var escapeNext = false
        var foundOpenBrace = false
        var foundDeclarationKeyword = false

        // Keywords that indicate we're past annotations and at the actual declaration
        val declarationKeywords = listOf("class", "interface", "object", "fun", "val", "var", "typealias")

        for ((index, char) in restOfFile.withIndex()) {
            // Check if we've reached a declaration keyword
            if (!foundDeclarationKeyword) {
                for (keyword in declarationKeywords) {
                    if (restOfFile.substring(index).startsWith(keyword) &&
                        (index + keyword.length >= restOfFile.length ||
                         restOfFile[index + keyword.length].isWhitespace() ||
                         restOfFile[index + keyword.length] == '<')) {
                        foundDeclarationKeyword = true
                        break
                    }
                }
            }

            when {
                escapeNext -> escapeNext = false
                char == '\\' -> escapeNext = true
                char == '"' && !inChar -> inString = !inString
                char == '\'' && !inString -> inChar = !inChar
                !inString && !inChar -> {
                    when (char) {
                        '{' -> {
                            braceCount++
                            foundOpenBrace = true
                        }
                        '}' -> {
                            braceCount--
                            if (foundOpenBrace && braceCount == 0) {
                                // Found matching closing brace
                                val endPosition = minOf(startPosition + index + 1, content.length - 1)
                                return findLineNumber(content, endPosition)
                            }
                        }
                    }
                }
            }

            // For declarations without braces (properties, typealiases), stop at newline
            // But only if we've found the declaration keyword
            if (foundDeclarationKeyword && !foundOpenBrace && char == '\n' && index > 0) {
                val endPosition = minOf(startPosition + index, content.length - 1)
                return findLineNumber(content, endPosition)
            }
        }

        // Fallback: use the start line + some buffer
        return minOf(startLine + 1, lines.size)
    }

    private fun determineKind(declarationText: String): DeclarationKind {
        return when {
            declarationText.contains("class ") -> DeclarationKind.CLASS
            declarationText.contains("interface ") -> DeclarationKind.INTERFACE
            declarationText.contains("object ") -> DeclarationKind.OBJECT
            declarationText.contains("fun ") -> DeclarationKind.FUNCTION
            declarationText.contains("val ") || declarationText.contains("var ") -> DeclarationKind.PROPERTY
            declarationText.contains("typealias ") -> DeclarationKind.TYPEALIAS
            else -> DeclarationKind.CLASS // fallback
        }
    }

    private fun computeSignatureHash(declarationText: String): String {
        val signature = extractSignature(declarationText)
        val normalized = signature
            .replace(Regex("\\s+"), " ")
            .trim()
        return normalized.hashCode().toString()
    }

    private fun extractSignature(declarationText: String): String {
        // Remove KDoc comments
        val withoutKDoc = declarationText.replace(Regex("/\\*\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "").trim()

        return when {
            withoutKDoc.contains("fun ") -> extractFunctionSignature(withoutKDoc)
            withoutKDoc.contains("class ") || withoutKDoc.contains("interface ") ||
            withoutKDoc.contains("object ") -> extractClassSignature(withoutKDoc)
            withoutKDoc.contains("val ") || withoutKDoc.contains("var ") -> extractPropertySignature(withoutKDoc)
            withoutKDoc.contains("typealias ") -> extractTypeAliasSignature(withoutKDoc)
            else -> withoutKDoc
        }
    }

    private fun extractFunctionSignature(text: String): String {
        val bodyStart = text.indexOf('{')
        val expressionBodyStart = text.indexOf('=')

        return when {
            bodyStart > 0 && (expressionBodyStart < 0 || bodyStart < expressionBodyStart) ->
                text.substring(0, bodyStart).trim()
            expressionBodyStart > 0 ->
                text.substring(0, expressionBodyStart + 1).trim()
            else -> text
        }
    }

    private fun extractClassSignature(text: String): String {
        val bodyStart = text.indexOf('{')
        return if (bodyStart > 0) text.substring(0, bodyStart).trim() else text
    }

    private fun extractPropertySignature(text: String): String {
        val getterStart = text.indexOf("get()")
        val setterStart = text.indexOf("set(")

        return when {
            getterStart > 0 -> text.substring(0, getterStart).trim()
            setterStart > 0 -> text.substring(0, setterStart).trim()
            else -> text
        }
    }

    private fun extractTypeAliasSignature(text: String): String {
        return text.trim()
    }

    private suspend fun getCommitHistory(file: File, gitRoot: File): List<CommitInfo> {
        val relativePath = file.relativeTo(gitRoot).path

        // Use --follow to track across renames, and get path history
        val command = "git log --follow --format=%H|%ad --date=short --name-only -- $relativePath"

        val result = runCommand(
            command = command,
            workingDir = gitRoot,
            timeoutAmount = 60.seconds,
            exitOnError = false
        )

        if (!result.isSuccess) {
            if (verbose) {
                printlnWarn("   Failed to get commit history for $relativePath")
            }
            return emptyList()
        }

        return parseCommitLog(result.output, relativePath)
    }

    private fun parseCommitLog(output: String, defaultPath: String): List<CommitInfo> {
        val commits = mutableListOf<CommitInfo>()
        val lines = output.trim().split("\n")

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                i++
                continue
            }

            val parts = line.split("|")
            if (parts.size == 2) {
                val hash = parts[0]
                val dateStr = parts[1]

                try {
                    val date = LocalDate.parse(dateStr, Config.DATE_FORMATTER)

                    // Next line might be the file path (from --name-only)
                    var filePath = defaultPath
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1].trim()
                        if (nextLine.isNotEmpty() && !nextLine.contains("|")) {
                            filePath = nextLine
                            i++ // Skip the path line
                        }
                    }

                    commits.add(CommitInfo(hash, date, filePath))
                } catch (e: Exception) {
                    if (verbose) {
                        printlnWarn("   Failed to parse date: $dateStr")
                    }
                }
            }
            i++
        }

        return commits
    }

    private suspend fun trackDeclarationHistory(
        declaration: DeclarationInfo,
        commits: List<CommitInfo>,
        gitRoot: File
    ): List<HistoricalDeclaration> {
        val history = mutableListOf<HistoricalDeclaration>()

        for (commit in commits) {
            val fileContent = getFileAtCommit(commit.hash, commit.filePath, gitRoot)

            if (fileContent == null) {
                history.add(HistoricalDeclaration(
                    commit = commit.hash,
                    date = commit.date,
                    exists = false,
                    hasAnnotation = false,
                    fullText = null,
                    contentHash = null
                ))
                continue
            }

            val matchedDecl = findMatchingDeclaration(
                targetName = declaration.name,
                targetSignatureHash = declaration.signatureHash,
                fileContent = fileContent,
                targetKind = declaration.kind
            )

            if (matchedDecl == null) {
                history.add(HistoricalDeclaration(
                    commit = commit.hash,
                    date = commit.date,
                    exists = false,
                    hasAnnotation = false,
                    fullText = null,
                    contentHash = null
                ))
                continue
            }

            val hasAnnotation = matchedDecl.contains(Config.ANNOTATION_NAME)

            history.add(HistoricalDeclaration(
                commit = commit.hash,
                date = commit.date,
                exists = true,
                hasAnnotation = hasAnnotation,
                fullText = matchedDecl,
                contentHash = computeContentHash(matchedDecl)
            ))
        }

        return history
    }

    private suspend fun getFileAtCommit(commit: String, path: String, gitRoot: File): String? {
        val cacheKey = commit to path

        return commitFileCache.getOrPut(cacheKey) {
            val command = "git show $commit:$path"
            val result = runCommand(
                command = command,
                workingDir = gitRoot,
                timeoutAmount = 10.seconds,
                exitOnError = false
            )

            if (result.isSuccess) result.output else null
        }
    }

    private fun findMatchingDeclaration(
        targetName: String,
        targetSignatureHash: String,
        fileContent: String,
        targetKind: DeclarationKind
    ): String? {
        // Extract all declarations of the same kind from the file content
        val declarations = extractAllDeclarationsFromContent(fileContent, targetKind)

        // Strategy 1: Name match (primary strategy, works across annotation changes)
        val nameMatches = declarations.filter { decl ->
            extractDeclarationName(decl) == targetName
        }

        if (nameMatches.size == 1) {
            return nameMatches.first()
        }

        // Strategy 2: Exact signature hash match (for confirmation/verification)
        val exactMatch = declarations.find {
            computeSignatureHash(it) == targetSignatureHash
        }
        if (exactMatch != null) return exactMatch

        // For multiple name matches (overloads), try fuzzy signature matching
        if (nameMatches.size > 1) {
            // Try to match by comparing function parameters for overloads
            // For now, just return the first match
            return nameMatches.firstOrNull()
        }

        return null
    }

    private fun extractAllDeclarationsFromContent(content: String, kind: DeclarationKind): List<String> {
        val declarations = mutableListOf<String>()

        for (pattern in Config.DECLARATION_PATTERNS) {
            pattern.findAll(content).forEach { match ->
                val matchStart = match.range.first
                val actualStart = findDeclarationStart(content, matchStart)
                val lineStart = findLineNumber(content, actualStart)
                val lineEnd = findDeclarationEnd(content, matchStart)

                val declText = content.lines().subList(lineStart - 1, minOf(lineEnd, content.lines().size)).joinToString("\n")
                val declKind = determineKind(declText)

                if (declKind == kind) {
                    declarations.add(declText)
                }
            }
        }

        return declarations
    }

    private fun extractDeclarationName(declarationText: String): String? {
        for (pattern in Config.DECLARATION_PATTERNS) {
            val match = pattern.find(declarationText)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun computeContentHash(declarationText: String): String {
        // For content hash, use the full declaration including body
        // This ensures we detect all changes (signature AND body)
        val normalized = declarationText
            .replace(Regex("\\s+"), " ")
            .trim()
        return normalized.hashCode().toString()
    }

    private fun normalizeDeclarationWithoutApiStatusExperimental(declarationText: String): String {
        // Remove @ApiStatus.Experimental and @ExperimentalJewelApi annotations for comparison
        // This allows us to detect if only @ApiStatus.Experimental was added
        return declarationText
            .replace(Regex("@ApiStatus\\.Experimental(?:\\([^)]*\\))?\\s*"), "")
            .replace(Regex("@ExperimentalJewelApi(?:\\([^)]*\\))?\\s*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isOnlyApiStatusExperimentalChange(olderText: String?, newerText: String?): Boolean {
        // If either is null, it's not just an annotation change
        if (olderText == null || newerText == null) return false

        // Normalize both texts by removing @ApiStatus.Experimental and @ExperimentalJewelApi
        val normalizedOlder = normalizeDeclarationWithoutApiStatusExperimental(olderText)
        val normalizedNewer = normalizeDeclarationWithoutApiStatusExperimental(newerText)

        // Check if the only difference is @ApiStatus.Experimental
        // This is true if: normalized versions are same AND @ApiStatus.Experimental exists in only one
        val olderHasApiStatus = olderText.contains("@ApiStatus.Experimental")
        val newerHasApiStatus = newerText.contains("@ApiStatus.Experimental")

        return normalizedOlder == normalizedNewer && (olderHasApiStatus != newerHasApiStatus)
    }

    private fun extractLifecycleDates(
        declaration: DeclarationInfo,
        history: List<HistoricalDeclaration>
    ): AnnotationLifecycle? {
        if (history.isEmpty()) return null

        // Find LAST MODIFIED: First commit where content differs from HEAD
        // Skip changes that only add @ApiStatus.Experimental annotation
        var lastModifiedCommit: String? = null
        var lastModifiedDate: LocalDate? = null
        val headContentHash = history.firstOrNull()?.contentHash

        for (i in 1 until history.size) {
            if (history[i].contentHash != headContentHash) {
                // Check if the only difference is the addition of @ApiStatus.Experimental
                val newerText = history[i - 1].fullText
                val olderText = history[i].fullText

                if (isOnlyApiStatusExperimentalChange(olderText, newerText)) {
                    // This change only added @ApiStatus.Experimental, skip it
                    if (verbose) {
                        println("     Skipping @ApiStatus.Experimental-only change at ${history[i - 1].date}")
                    }
                    continue
                }

                lastModifiedCommit = history[i - 1].commit
                lastModifiedDate = history[i - 1].date
                break  // Stop once we find the first real change
            }
        }

        // If never changed, use the oldest commit where it exists with annotation
        if (lastModifiedCommit == null) {
            val oldestWithAnnotation = history.lastOrNull { it.hasAnnotation }
            if (oldestWithAnnotation != null) {
                lastModifiedCommit = oldestWithAnnotation.commit
                lastModifiedDate = oldestWithAnnotation.date
            }
        }

        if (lastModifiedCommit == null || lastModifiedDate == null) {
            return null
        }

        return AnnotationLifecycle(
            declarationName = declaration.name,
            file = declaration.file,
            lineNumber = declaration.lineStart,
            lastModifiedCommit = lastModifiedCommit,
            lastModifiedDate = lastModifiedDate,
            currentlyAnnotated = history.first().hasAnnotation
        )
    }

    private fun exportToCsv(lifecycles: List<AnnotationLifecycle>, outputFile: File) {
        print("⏳ Exporting to CSV: ${outputFile.absolutePath}...")

        outputFile.bufferedWriter().use { writer ->
            writer.write("Status,Declaration Name,File Path,Line Number,Last Modified,Age (Months),Age (Days),Last Commit\n")

            lifecycles.sortedBy { it.ageInMonths }.forEach { lifecycle ->
                val jewelRoot = findJewelRoot()
                val relativePath = if (jewelRoot != null) {
                    lifecycle.file.relativeTo(jewelRoot).path
                } else {
                    lifecycle.file.path
                }

                val status = lifecycle.getStatus(errorMonthsThreshold)

                writer.write(
                    "$status," +
                    "\"${lifecycle.declarationName}\"," +
                    "\"$relativePath\"," +
                    "${lifecycle.lineNumber}," +
                    "${lifecycle.lastModifiedDate}," +
                    "${lifecycle.ageInMonths}," +
                    "${lifecycle.ageInDays}," +
                    "\"${lifecycle.lastModifiedCommit}\"\n"
                )
            }
        }

        println(" DONE")
    }

    private fun reportSummary(scanResult: ScanResult) {
        val lifecycles = scanResult.lifecycles

        // Calculate status breakdown
        val errorCount = lifecycles.count { it.ageInMonths >= errorMonthsThreshold }
        val warningThreshold = (errorMonthsThreshold * 0.75).toLong()
        val warningCount = lifecycles.count { it.ageInMonths >= warningThreshold && it.ageInMonths < errorMonthsThreshold }
        val okCount = lifecycles.count { it.ageInMonths < warningThreshold }

        println()
        println("=".repeat(70))
        println("Summary Report")
        println("=".repeat(70))
        println("Files scanned:          ${scanResult.totalFilesScanned}")
        println("Files with annotations: ${scanResult.filesWithMatches}")
        println("Total declarations:     ${lifecycles.size}")
        println()
        println("Status breakdown:")
        println("  ❌ Error (>= $errorMonthsThreshold months):   $errorCount")
        println("  ⚠️  Warning (>= ${warningThreshold} months): $warningCount")
        println("  ✅ OK (< ${warningThreshold} months):       $okCount")
        println()
        println("Results exported to: ${File(output).absolutePath}")

        if (errorCount > 0) {
            println()
            if (errorCount <= 10) {
                println("All declarations exceeding threshold (sorted by age):")
                lifecycles.filter { it.ageInMonths >= errorMonthsThreshold }
                    .sortedByDescending { it.ageInMonths }
                    .forEach { lifecycle ->
                        println("  - ${lifecycle.declarationName.padEnd(40)} ${lifecycle.ageInMonths} months (last modified: ${lifecycle.lastModifiedDate})")
                    }
            } else {
                println("Top 10 oldest declarations exceeding threshold:")
                lifecycles.filter { it.ageInMonths >= errorMonthsThreshold }
                    .sortedByDescending { it.ageInMonths }
                    .take(10)
                    .forEach { lifecycle ->
                        println("  - ${lifecycle.declarationName.padEnd(40)} ${lifecycle.ageInMonths} months (last modified: ${lifecycle.lastModifiedDate})")
                    }
            }
        }

        println("=".repeat(70))
    }
}

// Entry point
runBlocking { FindExperimentalApiCommand().main(args) }