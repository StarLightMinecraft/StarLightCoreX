package io.hikarilan.starlightcorex.generic

import io.hikarilan.starlightcorex.person.Human
import java.util.*

/**
 * 该接口指明对象可检查 Permissible 是否拥有指定权限
 */
interface ProxiedPermissible<T : Unique> {

    fun hasPermission(t: T, permission: String): Boolean

}

interface ProxiedPermissibleDefaultImpl<T : Unique> {

    // 玩家和权限
    val scopedPermissions: MutableMap<UUID, MutableList<String>>

    // 组和组权限
    val groupPermission: MutableMap<String, MutableList<String>>
    // 玩家和组
    val scopedGroup: MutableMap<UUID, String>

    fun hasPermission(t: T, permission: String): Boolean {
        return scopedPermissions[t.uniqueId]?.contains(permission)
            ?: groupPermission[scopedGroup[t.uniqueId]]?.contains(permission)
            ?: false
    }

}