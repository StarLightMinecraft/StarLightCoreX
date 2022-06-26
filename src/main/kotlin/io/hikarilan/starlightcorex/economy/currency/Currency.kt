package io.hikarilan.starlightcorex.economy.currency

import io.hikarilan.starlightcorex.generic.Unique
import java.math.BigDecimal

sealed interface Currency : Unique {

    val type:Type

    var name: String

    var symbol: String

    fun format(amount: BigDecimal): String

    enum class Type {
        LOCAL,
        REMOTE
    }

}

interface LocalCurrency : Currency{

    override val type: Currency.Type
        get() = Currency.Type.LOCAL

}

interface RemoteCurrency : Currency {

    override val type: Currency.Type
        get() = Currency.Type.REMOTE

    val linkedCurrency: LocalCurrency

}