package code.name.monkey.retromusic.util

import java.text.Collator
import java.util.Locale

object SortingUtil {
    private val collator = Collator.getInstance()

    // Common articles/prefixes to ignore when sorting
    private val IGNORED_PREFIXES = listOf(
        "the ", "a ", "an ", "los ", "las ", "el ", "la ", "le ", "les ", "der ", "die ", "das "
    ).map { it.lowercase(Locale.getDefault()) }

    /**
     * Removes common prefixes from a string for sorting purposes
     */
    fun getSortableString(value: String, sortValue: String? = null): String {
        // If a sort value is provided (from tags), use it
        if (!sortValue.isNullOrBlank()) {
            return sortValue
        }

        // Otherwise, remove common prefixes
        return removePrefixes(value)
    }

    /**
     * Removes common prefixes like "The", "A", "An" from the beginning of a string
     */
    fun removePrefixes(value: String): String {
        val trimmed = value.trim()
        val lowercase = trimmed.lowercase(Locale.getDefault())

        for (prefix in IGNORED_PREFIXES) {
            if (lowercase.startsWith(prefix)) {
                return trimmed.substring(prefix.length).trim()
            }
        }

        return trimmed
    }

    /**
     * Gets the last word from an artist name for "sort by last name" functionality
     */
    fun getLastName(artistName: String): String {
        val cleaned = removePrefixes(artistName)
        val words = cleaned.split(" ").filter { it.isNotBlank() }
        return words.lastOrNull() ?: cleaned
    }

    /**
     * Compares two strings using locale-aware collation
     */
    fun compare(s1: String?, s2: String?): Int {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1
        return collator.compare(s1, s2)
    }
}

enum class ArtistSortMode {
    NAME,      // Sort by full name (minus prefixes)
    LAST_NAME  // Sort by last word in name
}