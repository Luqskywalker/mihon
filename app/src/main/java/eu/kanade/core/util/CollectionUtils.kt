package eu.kanade.core.util

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

/**
 * Utility functions for collection operations with performance optimizations
 * and enhanced functionality for Compose-based applications.
 */

/**
 * Inserts separators between elements in a list based on adjacent elements.
 *
 * @param T The original element type
 * @param R The resulting element type (must be a supertype of T)
 * @param generator Function that generates a separator based on the before and after elements
 * @return A new list with separators inserted between elements
 *
 * @sample eu.kanade.core.util.CollectionUtilsTest.testInsertSeparators
 */
inline fun <T : R, R : Any> List<T>.insertSeparators(
    crossinline generator: (before: T?, after: T?) -> R?,
): List<R> {
    if (isEmpty()) return emptyList()
    
    val newList = ArrayList<R>(size * 2) // Pre-allocate for performance
    
    for (i in -1..lastIndex) {
        val before = getOrNull(i)
        before?.let { newList.add(it) }
        
        val after = getOrNull(i + 1)
        val separator = generator(before, after)
        separator?.let { newList.add(it) }
    }
    
    return newList
}

/**
 * Inserts separators between elements in reverse order.
 * Useful for bottom-to-top layouts or reversed lists.
 *
 * @see insertSeparators
 */
inline fun <T : R, R : Any> List<T>.insertSeparatorsReversed(
    crossinline generator: (before: T?, after: T?) -> R?,
): List<R> {
    if (isEmpty()) return emptyList()
    
    val newList = ArrayList<R>(size * 2)
    
    for (i in size downTo 0) {
        val after = getOrNull(i)
        after?.let { newList.add(it) }
        
        val before = getOrNull(i - 1)
        val separator = generator(before, after)
        separator?.let { newList.add(it) }
    }
    
    return newList.asReversed()
}

/**
 * Conditional add or remove operation for HashSet.
 * 
 * @param value The value to add or remove
 * @param shouldAdd If true, adds the value; if false, removes it
 * @return true if the operation modified the set, false otherwise
 */
fun <E> HashSet<E>.addOrRemove(value: E, shouldAdd: Boolean): Boolean = 
    if (shouldAdd) add(value) else remove(value)

/**
 * Conditional add or remove operation for MutableSet with custom equality logic.
 */
inline fun <E> MutableSet<E>.addOrRemove(
    value: E,
    shouldAdd: Boolean,
    crossinline equalityCheck: (E, E) -> Boolean = { a, b -> a == b }
): Boolean {
    return if (shouldAdd) {
        add(value)
    } else {
        removeAll { equalityCheck(it, value) }
    }
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 * 
 * Uses Compose's fast iteration for better performance with random-access lists.
 * 
 * @throws IllegalArgumentException if used with non-random-access collections
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastFilterNot(predicate: (T) -> Boolean): List<T> {
    contract { callsInPlace(predicate) }
    require(this is RandomAccess) { "fastFilterNot should only be used with random-access lists" }
    return fastFilter { !predicate(it) }
}

/**
 * Splits the collection into two lists based on the predicate.
 * 
 * @return A Pair where first contains matching elements and second contains non-matching elements
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastPartition(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    contract { callsInPlace(predicate) }
    require(this is RandomAccess) { "fastPartition should only be used with random-access lists" }
    
    val first = ArrayList<T>()
    val second = ArrayList<T>()
    
    fastForEach { element ->
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    
    return first to second
}

/**
 * Returns the number of elements not matching the predicate.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastCountNot(predicate: (T) -> Boolean): Int {
    contract { callsInPlace(predicate) }
    require(this is RandomAccess) { "fastCountNot should only be used with random-access lists" }
    
    var count = 0
    fastForEach { if (!predicate(it)) count++ }
    return count
}

/**
 * Fast map operation with pre-allocated capacity.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T, R> List<T>.fastMap(crossinline transform: (T) -> R): List<R> {
    contract { callsInPlace(transform) }
    require(this is RandomAccess) { "fastMap should only be used with random-access lists" }
    
    val destination = ArrayList<R>(size)
    fastForEach { destination.add(transform(it)) }
    return destination
}

/**
 * Fast flatMap operation for nested collections.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T, R> List<T>.fastFlatMap(crossinline transform: (T) -> Iterable<R>): List<R> {
    contract { callsInPlace(transform) }
    require(this is RandomAccess) { "fastFlatMap should only be used with random-access lists" }
    
    val destination = ArrayList<R>()
    fastForEach { element ->
        destination.addAll(transform(element))
    }
    return destination
}

/**
 * Finds the first element matching the predicate using fast iteration.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastFirstOrNull(predicate: (T) -> Boolean): T? {
    contract { callsInPlace(predicate) }
    require(this is RandomAccess) { "fastFirstOrNull should only be used with random-access lists" }
    
    fastForEach { element ->
        if (predicate(element)) return element
    }
    return null
}

/**
 * Groups elements by key using fast iteration.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T, K> List<T>.fastGroupBy(crossinline keySelector: (T) -> K): Map<K, List<T>> {
    contract { callsInPlace(keySelector) }
    require(this is RandomAccess) { "fastGroupBy should only be used with random-access lists" }
    
    val destination = mutableMapOf<K, MutableList<T>>()
    fastForEach { element ->
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList() }
        list.add(element)
    }
    return destination
}

/**
 * Inline class for type-safe collection operations with performance guarantees.
 */
@JvmInline
value class FastList<T> private constructor(private val delegate: List<T>) : List<T> by delegate {
    
    init {
        require(delegate is RandomAccess) { "FastList requires random-access delegate" }
    }
    
    companion object {
        fun <T> from(list: List<T>): FastList<T> {
            return FastList(list)
        }
    }
    
    inline fun <R> map(crossinline transform: (T) -> R): List<R> = delegate.fastMap(transform)
    
    inline fun filter(crossinline predicate: (T) -> Boolean): List<T> = delegate.fastFilter(predicate)
    
    inline fun filterNot(crossinline predicate: (T) -> Boolean): List<T> = delegate.fastFilterNot(predicate)
    
    inline fun partition(crossinline predicate: (T) -> Boolean): Pair<List<T>, List<T>> = 
        delegate.fastPartition(predicate)
    
    inline fun countNot(crossinline predicate: (T) -> Boolean): Int = delegate.fastCountNot(predicate)
}

// Extension properties for easy conversion
val <T> List<T>.fast: FastList<T>
    get() = FastList.from(this)

/**
 * Utility for building lists with separators in a builder pattern.
 */
class SeparatorListBuilder<T : R, R : Any> private constructor(
    private val original: List<T>
) {
    private val result = mutableListOf<R>()
    
    fun addSeparator(before: T?, after: T?, separator: R) = apply {
        result.add(separator)
    }
    
    fun build(): List<R> = result
    
    companion object {
        fun <T : R, R : Any> from(list: List<T>): SeparatorListBuilder<T, R> {
            return SeparatorListBuilder(list)
        }
    }
}
