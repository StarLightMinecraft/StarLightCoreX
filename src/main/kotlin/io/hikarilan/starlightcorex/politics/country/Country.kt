package io.hikarilan.starlightcorex.politics.country

import io.hikarilan.starlightcorex.economy.account.AccountHolder
import io.hikarilan.starlightcorex.politics.country.account.CountryAccount
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.economy.currency.CurrencyManager
import io.hikarilan.starlightcorex.generic.*
import io.hikarilan.starlightcorex.generic.result.Failure
import io.hikarilan.starlightcorex.generic.result.Response
import io.hikarilan.starlightcorex.generic.result.Success
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.region.Region
import io.hikarilan.starlightcorex.region.RegionHolderDefaultImpl
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.DirectMailReceiver
import io.hikarilan.starlightcorex.utils.Mail
import io.hikarilan.starlightcorex.utils.ProxiedMailReceiver
import io.hikarilan.starlightcorex.utils.primaryColor
import net.kyori.adventure.text.Component
import java.util.*

data class Country(
    override val uniqueId: UUID = UUID.randomUUID(),
    override var name: String = "国家#${uniqueId.toString().substringAfterLast('-')}",
    override val account: CountryAccount = CountryAccount(uniqueId),
    override val regions: MutableList<Region> = mutableListOf(),
    override val extra: MutableMap<UUID, MutableMap<String, Any>> = mutableMapOf(),
    override val members: MutableList<UUID> = mutableListOf(),
    override var currency: Currency? = null,
    override val scopedPermissions: MutableMap<UUID, MutableList<String>> = mutableMapOf(),
    override val groupPermission: MutableMap<String, MutableList<String>> = mutableMapOf(
        // -- 白名单 --
        // 平民
        "commoner" to mutableListOf(),
        // -- 黑名单 --
        // 特权阶层
        "privileged" to mutableListOf(),
        // 统治阶层
        "ruling" to mutableListOf()
    ),
    override val scopedGroup: MutableMap<UUID, String> = mutableMapOf(),
    private val settings: MutableMap<String, Any> = mutableMapOf(
        "isAllowJoin" to true,
        "isJoinNeedPending" to true,
    )
) : Unique, Nameable, AccountHolder, ProxiedPermissibleDefaultImpl<Human>, ProxiedMailReceiver,
    RootSerializable, RegionHolderDefaultImpl, PersistentMemberHolder<Human>, CurrencyManager {

    @delegate:Transient
    var isAllowJoin: Boolean by settings

    @delegate:Transient
    var isJoinNeedPending: Boolean by settings

    val joinPending: MutableMap<UUID, Long> = mutableMapOf()

    var enterMessage: Component? = Component.text("欢迎来到 {country}！")
        get() = field?.replaceText { it.matchLiteral("{country}").replacement(this.name) }

    var leaveMessage: Component? = Component.text("您已经离开 {country}！")
        get() = field?.replaceText { it.matchLiteral("{country}").replacement(this.name) }

    override val receivers: List<DirectMailReceiver>
        get() = members.mapNotNull { getStorageFor<Human>(it) }


    override fun hasPermission(t: Human, permission: String): Boolean {
        return if (t.technicalPlayer.isOp) true
        else scopedPermissions[t.uniqueId]?.contains(permission)
            ?: when (scopedGroup[t.uniqueId]) {
                null -> false
                "privileged", "ruling" -> !(groupPermission[scopedGroup[t.uniqueId]]?.contains(permission) ?: false)
                else -> groupPermission[scopedGroup[t.uniqueId]]?.contains(permission) ?: false
            }
    }

    fun attemptToJoin(t: Human): Response {
        if (hasMember(t)) {
            return object : Response {
                override val isSuccess: Boolean = false
                override val reason: String = "您已是该国家成员，无需再次加入"
            }
        }
        if (joinPending.containsKey(t.uniqueId)) {
            return object : Response {
                override val isSuccess: Boolean = false
                override val reason: String = "您已经申请加入该国家，不要多次申请"
            }
        }
        if (regions.none { it.checkIn(t) }) {
            return object : Response {
                override val isSuccess: Boolean = false
                override val reason: String = "您不在该国家的领土范围内"
            }
        }
        if (!isAllowJoin) {
            return object : Response {
                override val isSuccess: Boolean = false
                override val reason: String = "该国家目前不允许任何人加入"
            }
        }
        if (isJoinNeedPending) {
            joinPending[t.uniqueId] = System.currentTimeMillis()
            members.mapNotNull { getStorageFor<Human>(it) }.filter { hasPermission(it, "country.check_join_pending") }
                .forEach {
                    it.sendMail(
                        Mail(
                            message = Component.text("您收到了新的国家加入申请，请进入国家管理面板查看").color(primaryColor),
                            expiredWhen = Long.MAX_VALUE
                        )
                    )
                }
            return object : Response {
                override val isSuccess: Boolean = true
                override val reason: String = "已收到加入申请，请等待该国家相关人员审核"
            }
        }
        addMember(t)
        return Success
    }


}