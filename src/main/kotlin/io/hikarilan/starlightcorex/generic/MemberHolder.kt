package io.hikarilan.starlightcorex.generic

import java.util.UUID

interface MemberHolder<T : Unique> : ExtraHolderDefaultImpl<T> {

    fun addMember(t: T)

    fun hasMember(t: T): Boolean

    fun removeMember(t: T)

}

interface MemberHolderDefaultImpl<T : Unique> : MemberHolder<T> {

    val members: MutableList<UUID>

    val isExtraPersistent: Boolean

    override fun addMember(t: T) {
        members.add(t.uniqueId)
    }

    override fun hasMember(t: T): Boolean {
        return members.contains(t.uniqueId)
    }

    override fun removeMember(t: T) {
        members.remove(t.uniqueId)
        if (!isExtraPersistent) removeAllExtra(t)
    }
}

/**
 * 该 MemberHolder 中的 Extra 将会在 Member 被 remove 时仍然保留
 */
interface PersistentMemberHolder<T : Unique> : MemberHolderDefaultImpl<T> {

    override val isExtraPersistent: Boolean
        get() = true

}

/**
 * 该 MemberHolder 中的 Extra 将会在 Member 被 remove 时被清除
 */
interface TransientMemberHolder<T : Unique> : MemberHolderDefaultImpl<T> {

    override val isExtraPersistent: Boolean
        get() = false

}