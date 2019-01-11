package com.smallhacker.disbrowser.resource

import com.smallhacker.disbrowser.Service
import com.smallhacker.disbrowser.asm.Address
import com.smallhacker.disbrowser.asm.MetadataLine
import com.smallhacker.disbrowser.util.toUInt24
import com.smallhacker.disbrowser.util.tryParseInt
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/rest")
class RestResource {
    @POST
    @Path("{address}/{field}")
    @Consumes(MediaType.TEXT_PLAIN)
    fun getIt(@PathParam("address") address: String, @PathParam("field") fieldName: String, body: String): Response {
        val parsedAddress = parseAddress(address) ?: return Response.status(400).build()
        val field = when (fieldName) {
            "preComment" -> MetadataLine::preComment
            "comment" -> MetadataLine::comment
            "label" -> MetadataLine::label
            else -> return Response.status(404).build()
        }

        Service.updateMetadata(parsedAddress, field, body)

        return Response.noContent().build()
    }


    private fun parseAddress(address: String): Address? {
        return tryParseInt(address, 16)
                ?.let { Address(it.toUInt24()) }
    }

}
