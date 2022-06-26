package io.hikarilan.starlightcorex.generic.event

import io.hikarilan.starlightcorex.person.Human
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

abstract class HumanEvent(
   val human: Human
) : Event() {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }

    override fun getHandlers(): HandlerList {
        return HumanEvent.handlers
    }


}