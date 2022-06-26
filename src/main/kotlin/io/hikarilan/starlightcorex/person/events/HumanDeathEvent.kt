package io.hikarilan.starlightcorex.person.events

import io.hikarilan.starlightcorex.generic.event.HumanEvent
import io.hikarilan.starlightcorex.person.Human

class HumanDeathEvent(human: Human) : HumanEvent(human)