package io.hikarilan.starlightcorex.generic.source

interface ActionSource {

    val name: String

    val reason: String

    val isForce: Boolean
        get() = false

}

/**
 * 使用此 ActionSource 来表明一个应当强制执行的行动
 */
interface ForceActionSource : ActionSource {

    override val isForce: Boolean
        get() = true

}