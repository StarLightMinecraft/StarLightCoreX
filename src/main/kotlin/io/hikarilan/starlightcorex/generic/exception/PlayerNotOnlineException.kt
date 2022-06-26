package io.hikarilan.starlightcorex.generic.exception

import io.hikarilan.starlightcorex.person.TechnicalPlayer

class PlayerNotOnlineException(
    player: TechnicalPlayer
) : IllegalStateException("Player ${player.uniqueId} is not online")