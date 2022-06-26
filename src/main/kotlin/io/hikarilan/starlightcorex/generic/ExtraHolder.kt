package io.hikarilan.starlightcorex.generic

import java.util.UUID

interface ExtraHolder<T : Unique> {

    fun putExtra(t: T, key: String, value: Any)

    fun hasExtra(t: T, key: String): Boolean

    fun getExtra(t: T, key: String): Any?

    fun removeExtra(t: T, key: String)

    fun removeAllExtra(t: T)

}

interface ExtraHolderDefaultImpl<T : Unique> : ExtraHolder<T> {

    val extra: MutableMap<UUID, MutableMap<String, Any>>

    override fun putExtra(t: T, key: String, value: Any) {
        extra.computeIfAbsent(t.uniqueId) { mutableMapOf() }[key] = value
    }

    override fun hasExtra(t: T, key: String): Boolean {
        return extra[t.uniqueId]?.containsKey(key) ?: false
    }

    override fun getExtra(t: T, key: String): Any? {
        return extra[t.uniqueId]?.get(key)
    }

    override fun removeExtra(t: T, key: String) {
        extra[t.uniqueId]?.remove(key)
    }

    override fun removeAllExtra(t: T) {
        extra.remove(t.uniqueId)
    }

}