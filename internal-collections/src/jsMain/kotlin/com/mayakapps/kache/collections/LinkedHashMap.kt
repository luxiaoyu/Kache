/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * A copy of LinkedHashMap.kt from Kotlin 1.8.20 from:
 *   https://github.com/JetBrains/kotlin/blob/v1.8.20/libraries/stdlib/js/src/kotlin/collections/LinkedHashMap.kt
 * Changes are marked with "MODIFICATION" comments. Note that:
 *   - "actual " was replaced by "" (empty string) throughout the file
 */

/*
 * Based on GWT LinkedHashMap
 * Copyright 2008 Google Inc.
 */

// MODIFICATION: package renamed to com.mayakapps.kache.collections
package com.mayakapps.kache.collections

import kotlin.collections.MutableMap.MutableEntry

/**
 * Hash table based implementation of the [MutableMap] interface, which additionally preserves the insertion order
 * of entries during the iteration.
 *
 * The insertion order is preserved by maintaining a doubly-linked list of all of its entries.
 */
public open class LinkedHashMap<K, V> : HashMap<K, V>, MutableMap<K, V> {

    /**
     * The entry we use includes next/prev pointers for a doubly-linked circular
     * list with a head node. This reduces the special cases we have to deal with
     * in the list operations.

     * Note that we duplicate the key from the underlying hash map so we can find
     * the eldest entry. The alternative would have been to modify HashMap so more
     * of the code was directly usable here, but this would have added some
     * overhead to HashMap, or to reimplement most of the HashMap code here with
     * small modifications. Paying a small storage cost only if you use
     * LinkedHashMap and minimizing code size seemed like a better tradeoff
     */
    private inner class ChainEntry<K, V>(key: K, value: V) : AbstractMutableMap.SimpleEntry<K, V>(key, value) {
        internal var next: ChainEntry<K, V>? = null
        internal var prev: ChainEntry<K, V>? = null

        override fun setValue(newValue: V): V {
            this@LinkedHashMap.checkIsMutable()
            return super.setValue(newValue)
        }
    }

    private inner class EntrySet : AbstractEntrySet<MutableEntry<K, V>, K, V>() {

        private inner class EntryIterator : MutableIterator<MutableEntry<K, V>> {
            // The last entry that was returned from this iterator.
            private var last: ChainEntry<K, V>? = null

            // The next entry to return from this iterator.
            private var next: ChainEntry<K, V>? = null

            init {
                next = head
//                recordLastKnownStructure(map, this)
            }

            override fun hasNext(): Boolean {
                return next !== null
            }

            override fun next(): MutableEntry<K, V> {
//                checkStructuralChange(map, this)
                if (!hasNext()) throw NoSuchElementException()

                val current = next!!
                last = current
                next = current.next.takeIf { it !== head }
                return current
            }

            override fun remove() {
                check(last != null)
                // MODIFICATION: comment out the following line as we're not going to use read-only collections
//                this@EntrySet.checkIsMutable()
//                checkStructuralChange(map, this)

                last!!.remove()
                map.remove(last!!.key)
//                recordLastKnownStructure(map, this)
                last = null
            }
        }

        // MODIFICATION: add a reverse iterator
        private inner class ReverseEntryIterator : MutableIterator<MutableEntry<K, V>> {
            // The last entry that was returned from this iterator.
            private var last: ChainEntry<K, V>? = null

            // The next entry to return from this iterator.
            private var next: ChainEntry<K, V>? = null

            init {
                // MODIFICATION: Use tail instead of head
                next = head?.prev
//                recordLastKnownStructure(map, this)
            }

            override fun hasNext(): Boolean {
                return next !== null
            }

            override fun next(): MutableEntry<K, V> {
//                checkStructuralChange(map, this)
                if (!hasNext()) throw NoSuchElementException()

                val current = next!!
                last = current
                // MODIFICATION: Use prev instead of next and check current instead of it
                next = current.prev.takeIf { current !== head }
                return current
            }

            override fun remove() {
                check(last != null)
                // MODIFICATION: comment out the following line as we're not going to use read-only collections
//                this@EntrySet.checkIsMutable()
//                checkStructuralChange(map, this)

                last!!.remove()
                map.remove(last!!.key)
//                recordLastKnownStructure(map, this)
                last = null
            }
        }

        override fun add(element: MutableEntry<K, V>): Boolean =
            throw UnsupportedOperationException("Add is not supported on entries")

        override fun clear() {
            this@LinkedHashMap.clear()
        }

        override fun containsEntry(element: Map.Entry<K, V>): Boolean = this@LinkedHashMap.containsEntry(element)

        // MODIFICATION: add a reverseOrder condition
        override operator fun iterator(): MutableIterator<MutableEntry<K, V>> = if (!reverseOrder) EntryIterator() else ReverseEntryIterator()

        override fun removeEntry(element: Map.Entry<K, V>): Boolean {
            checkIsMutable()
            if (contains(element)) {
                this@LinkedHashMap.remove(element.key)
                return true
            }
            return false
        }

        override val size: Int get() = this@LinkedHashMap.size

        // MODIFICATION: comment out this function as we're not going to use read-only collections
//        override fun checkIsMutable(): Unit = this@LinkedHashMap.checkIsMutable()
    }


    /*
   * The head of the insert order chain, which is a doubly-linked circular
   * list.
   *
   * The most recently inserted node is at the end of the chain, ie.
   * chain.prev.
   */
    private var head: ChainEntry<K, V>? = null

    /**
     * Add this node to the end of the chain.
     */
    private fun ChainEntry<K, V>.addToEnd() {
        // This entry is not in the list.
        check(next == null && prev == null)

        val _head = head
        if (_head == null) {
            head = this
            next = this
            prev = this
        } else {
            // Chain is valid.
            val _tail = checkNotNull(_head.prev)
            // Update me.
            prev = _tail
            next = _head
            // Update my new siblings: current head and old tail
            _head.prev = this
            _tail.next = this
        }
    }

    /**
     * Remove this node from the chain it is a part of.
     */
    private fun ChainEntry<K, V>.remove() {
        if (this.next === this) {
            // if this is single element, remove head
            head = null
        } else {
            if (head === this) {
                // if this is first element, move head to next
                head = next
            }
            next!!.prev = prev
            prev!!.next = next
        }
        next = null
        prev = null
    }

    // MODIFICATION: add this
    private fun ChainEntry<K, V>.afterAccess() {
        var _head = checkNotNull(head)
        val _tail = checkNotNull(_head.prev)

        // if this is the tail or we don't care about access order, return
        if (!accessOrder || this === _tail) return

        // remove this from the chain
        next!!.prev = prev
        prev!!.next = next
        if (_head === this) {
            head = next
            _head = checkNotNull(head)
        }

        // add this to the end of the chain
        prev = _tail
        next = _head
        _head.prev = this
        _tail.next = this
    }

    /*
   * The hashmap that keeps track of our entries and the chain. Note that we
   * duplicate the key here to eliminate changes to HashMap and minimize the
   * code here, at the expense of additional space.
   */
    private val map: HashMap<K, ChainEntry<K, V>>

    // MODIFICATION: add these variables
    private val accessOrder: Boolean
    private val reverseOrder: Boolean

    private var isReadOnly: Boolean = false

    /**
     * Constructs an empty [LinkedHashMap] instance.
     */
    constructor() : super() {
        map = HashMap<K, ChainEntry<K, V>>()

        // MODIFICATION: add the next two lines
        accessOrder = false
        reverseOrder = false
    }

    internal constructor(backingMap: HashMap<K, Any>) : super() {
        @Suppress("UNCHECKED_CAST") // expected to work due to erasure
        map = backingMap as HashMap<K, ChainEntry<K, V>>

        // MODIFICATION: add the next two lines
        accessOrder = false
        reverseOrder = false
    }

    /**
     * Constructs an empty [LinkedHashMap] instance.
     *
     * @param  initialCapacity the initial capacity (ignored)
     * @param  loadFactor      the load factor (ignored)
     *
     * @throws IllegalArgumentException if the initial capacity or load factor are negative
     */
    constructor(initialCapacity: Int, loadFactor: Float, accessOrder: Boolean, reverseOrder: Boolean) : super(
        initialCapacity,
        loadFactor
    ) {
        map = HashMap<K, ChainEntry<K, V>>()

        // MODIFICATION: add the next two lines and the parameters
        this.accessOrder = accessOrder
        this.reverseOrder = reverseOrder
    }

    constructor(initialCapacity: Int) : this(initialCapacity, 0.0f, false, false)

    /**
     * Constructs an instance of [LinkedHashMap] filled with the contents of the specified [original] map.
     */
    constructor(original: Map<out K, V>) {
        map = HashMap<K, ChainEntry<K, V>>()
        this.putAll(original)

        // MODIFICATION: add the next two lines
        accessOrder = false
        reverseOrder = false
    }

    @PublishedApi
    internal fun build(): Map<K, V> {
        checkIsMutable()
        isReadOnly = true
        return this
    }

    override fun clear() {
        checkIsMutable()
        map.clear()
        head = null
    }


//    override fun clone(): Any {
//        return LinkedHashMap(this)
//    }


    override fun containsKey(key: K): Boolean = map.containsKey(key)

    override fun containsValue(value: V): Boolean {
        var node: ChainEntry<K, V> = head ?: return false
        do {
            if (node.value == value) {
                return true
            }
            node = node.next!!
        } while (node !== head)
        return false
    }


    internal override fun createEntrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = EntrySet()

    // MODIFICATION: add afterAccess call
    override operator fun get(key: K): V? = map.get(key)?.run { afterAccess(); value }

    override fun put(key: K, value: V): V? {
        checkIsMutable()

        val old = map.get(key)
        if (old == null) {
            val newEntry = ChainEntry(key, value)
            map.put(key, newEntry)
            newEntry.addToEnd()
            return null
        } else {
            // MODIFICATION: add afterAccess call
            return old.setValue(value).also { old.afterAccess() }
        }
    }

    override fun remove(key: K): V? {
        checkIsMutable()

        val entry = map.remove(key)
        if (entry != null) {
            entry.remove()
            return entry.value
        }
        return null
    }

    override val size: Int get() = map.size

    internal override fun checkIsMutable() {
        if (isReadOnly) throw UnsupportedOperationException()
    }
}

// MODIFICATION: comment out this function because it is not used

///**
// * Constructs the specialized implementation of [LinkedHashMap] with [String] keys, which stores the keys as properties of
// * JS object without hashing them.
// */
//public fun <V> linkedStringMapOf(vararg pairs: Pair<String, V>): LinkedHashMap<String, V> {
//    return LinkedHashMap<String, V>(stringMapOf<Any>()).apply { putAll(pairs) }
//}