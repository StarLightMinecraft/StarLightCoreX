package io.hikarilan.starlightcorex.utils.serialization

import com.google.gson.*
import io.hikarilan.starlightcorex.economy.currency.*
import io.hikarilan.starlightcorex.economy.currency.Currency
import java.lang.reflect.Type
import java.util.*

object CurrencySerializer : JsonDeserializer<Currency>, JsonSerializer<Currency> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Currency {
        return when (json.asJsonObject["type"]?.asString) {
            Currency.Type.LOCAL.toString() -> {
                CountryLocalCurrency(
                    name = json.asJsonObject["name"]?.asString ?: throw JsonParseException("Currency name is missing"),
                    symbol = json.asJsonObject["symbol"]?.asString
                        ?: throw JsonParseException("Currency symbol is missing"),
                    uniqueId = UUID.fromString(
                        json.asJsonObject["uniqueId"]?.asString
                            ?: throw JsonParseException("Currency uniqueId is missing")
                    ),
                )
            }
            Currency.Type.REMOTE.toString() -> {
                CountryRemoteCurrency(
                    uniqueId = UUID.fromString(
                        json.asJsonObject["uniqueId"]?.asString
                            ?: throw JsonParseException("Currency uniqueId is missing")
                    ),
                )
            }
            else -> throw JsonParseException("Currency type is null")
        }
    }

    override fun serialize(src: Currency?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return when (src) {
            is LocalCurrency -> {
                JsonObject().apply {
                    addProperty("type", Currency.Type.LOCAL.toString())
                    addProperty("name", src.name)
                    addProperty("symbol", src.symbol)
                    addProperty("uniqueId", src.uniqueId.toString())
                }
            }
            is RemoteCurrency -> {
                JsonObject().apply {
                    addProperty("type", Currency.Type.REMOTE.toString())
                    addProperty("uniqueId", src.uniqueId.toString())
                }
            }
            else -> throw JsonParseException("Currency type is null")
        }
    }
}