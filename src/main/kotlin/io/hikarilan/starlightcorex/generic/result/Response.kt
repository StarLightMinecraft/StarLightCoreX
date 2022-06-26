package io.hikarilan.starlightcorex.generic.result

interface Response {

    val isSuccess: Boolean

    val reason: String

}

object Success : Response {

    override val isSuccess: Boolean
        get() = true

    override val reason: String
        get() = "请求成功"

}

object Failure : Response {

    override val isSuccess: Boolean
        get() = false

    override val reason: String
        get() = "请求失败"

}

object Unsupported : Response {

    override val isSuccess: Boolean
        get() = false

    override val reason: String
        get() = "该请求不受支持"

}