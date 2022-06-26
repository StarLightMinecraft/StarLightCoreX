package io.hikarilan.starlightcorex.economy.result

import io.hikarilan.starlightcorex.economy.account.Account
import io.hikarilan.starlightcorex.generic.result.Response

interface EconomyResponse : Response {

    val account: Account

}

object Insufficient : Response {

    override val isSuccess: Boolean = false

    override val reason: String = "账户中没有足够的钱以完成操作"


}

object MinHoldings : Response {

    override val isSuccess: Boolean = false

    override val reason: String = "账户金额变更将使该账户低于允许的最低金额"

}

object MaxHoldings : Response {

    override val isSuccess: Boolean = false

    override val reason: String = "账户金额变更将使该账户超过允许的最高金额"

}