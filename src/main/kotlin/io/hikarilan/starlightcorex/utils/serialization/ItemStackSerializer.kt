package io.hikarilan.starlightcorex.utils.serialization

import com.google.gson.*
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type

object ItemStackSerializer : JsonDeserializer<ItemStack>, JsonSerializer<ItemStack> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemStack {
        return ItemStack.deserializeBytes(
            Gson().fromJson(json.asJsonPrimitive.asString, JsonArray::class.java).map { it.asByte }.toByteArray()
        )
    }

    override fun serialize(src: ItemStack?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val array = JsonArray()
        src?.serializeAsBytes()?.forEach { byt ->
            array.add(JsonPrimitive(byt))
        }
        return JsonPrimitive(array.toString())
    }
}