package io.hikarilan.starlightcorex.economy.account

import io.hikarilan.starlightcorex.generic.Nameable
import io.hikarilan.starlightcorex.generic.Permissible

interface AccountOperator : Nameable, Permissible

object SystemAccountOperator : AccountOperator {

    override var name: String = "系统账户管理员"

    override val groupPermission: MutableMap<String, MutableList<String>> = mutableMapOf()

    override fun hasPermission(permission: String): Boolean = true

    override fun addPermission(permission: String) {}

    override fun removePermission(permission: String) {}

    override fun setPermissionGroup(group: String?) {}

}