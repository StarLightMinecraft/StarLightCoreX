package io.hikarilan.starlightcorex.utils.serialization

import com.google.gson.*
import io.hikarilan.starlightcorex.person.ability.Ability
import io.hikarilan.starlightcorex.person.ability.BooleanAbility
import io.hikarilan.starlightcorex.person.ability.DoubleAbility
import java.lang.reflect.Type

object AbilitySerializer : JsonDeserializer<Ability<*>>, JsonSerializer<Ability<*>> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Ability<*> {
        return json.asJsonObject.let {
            if (it["value"] == null) {
                BooleanAbility(it["key"].asString, it["ambiguousName"].asString)
            } else if (it["value"].isJsonPrimitive && it["value"].asJsonPrimitive.isNumber) {
                DoubleAbility(it["key"].asString, it["value"].asDouble, it["ambiguousName"].asString)
            } else {
                throw IllegalArgumentException("Ability value is not a number or boolean")
            }
        }
    }

    override fun serialize(src: Ability<*>?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            addProperty("key", src?.key)
            when (src?.value) {
                is Boolean -> {
                    addProperty("ambiguousName", src.ambiguousName)
                }
                is Double -> {
                    addProperty("value", src.value as Double)
                    addProperty("ambiguousName", src.ambiguousName)
                }
                else -> addProperty("value", src?.value.toString())
            }
        }
    }
}