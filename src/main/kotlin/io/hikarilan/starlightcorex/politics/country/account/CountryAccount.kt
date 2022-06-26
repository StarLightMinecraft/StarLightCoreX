package io.hikarilan.starlightcorex.politics.country.account

import io.hikarilan.starlightcorex.economy.account.CommonAccount
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.politics.country.Country
import io.hikarilan.starlightcorex.storage.getStorageFor
import java.math.BigDecimal
import java.util.UUID

class CountryAccount(
    private val upstream: UUID,
    uniqueId: UUID = UUID.randomUUID(),
    holdings: MutableMap<UUID, BigDecimal> = mutableMapOf()
) : CommonAccount(uniqueId, holdings) {

    override val name: String
        get() = "${holder.name} 的国库"

    override val holder: Country
        get() = getStorageFor<Country>(upstream) ?: throw IllegalStateException("Country not found")

    override fun hasPermission(t: Human, permission: String): Boolean {
        return holder.hasPermission(t, permission)
    }


}