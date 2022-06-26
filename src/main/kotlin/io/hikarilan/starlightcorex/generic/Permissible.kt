package io.hikarilan.starlightcorex.generic

interface Permissible {

    val groupPermission: MutableMap<String, MutableList<String>>

    fun hasPermission(permission: String): Boolean

    fun addPermission(permission: String)

    fun removePermission(permission: String)

    fun setPermissionGroup(group: String?)

}

interface PermissibleDefaultImpl : Permissible {

    var group: String?
    val permissions: MutableSet<String>

    override fun hasPermission(permission: String): Boolean {
        return permissions.contains(permission) || groupPermission[group]?.contains(permission) == true
    }

    override fun addPermission(permission: String) {
        permissions.add(permission)
    }

    override fun removePermission(permission: String) {
        permissions.remove(permission)
    }

    override fun setPermissionGroup(group: String?) {
        this.group = group
    }

}