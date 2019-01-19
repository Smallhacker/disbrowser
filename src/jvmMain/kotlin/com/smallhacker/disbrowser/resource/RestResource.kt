package com.smallhacker.disbrowser.resource

import com.smallhacker.disbrowser.Service
import com.smallhacker.disbrowser.asm.SnesAddress
import com.smallhacker.disbrowser.asm.MetadataLine
import com.smallhacker.disbrowser.game.getGameSource
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Configuration
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/rest/{game}")
class RestResource {
    @Context
    private lateinit var config: Configuration
    private val games by lazy { config.getGameSource() }

    @POST
    @Path("{address}/{field}")
    @Consumes(MediaType.TEXT_PLAIN)
    fun getIt(
            @PathParam("game") gameName: String,
            @PathParam("address") address: String,
            @PathParam("field") fieldName: String,
            body: String
    ): Response {
        val parsedAddress = SnesAddress.parse(address) ?: return Response.status(400).build()
        val field = when (fieldName) {
            "preComment" -> MetadataLine::preComment
            "comment" -> MetadataLine::comment
            "label" -> MetadataLine::label
            else -> return http404()
        }

        val game = games.getGame(gameName)
                ?: return http404()
        Service.updateMetadata(game, parsedAddress, field, body)

        return Response.noContent().build()
    }

    private fun http404(): Response = Response.status(404).build()
}
