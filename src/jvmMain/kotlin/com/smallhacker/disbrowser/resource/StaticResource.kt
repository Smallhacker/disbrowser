package com.smallhacker.disbrowser.resource

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Response

@Path("/resources")
class StaticResource {
    @GET
    @Path("{file}.{ext}")
    fun getStatic(@PathParam("file") file: String, @PathParam("ext") ext: String): Response {
        val mime = when (ext) {
            "js" -> "application/javascript"
            "css" -> "text/css"
            else -> null
        }

        if (mime != null) {
            javaClass.getResourceAsStream("/public/$file.$ext")
                    ?.bufferedReader()
                    ?.use {
                        return Response.ok(it.readText())
                                .type(mime)
                                .build()
                    }
        }

        return Response.status(404).build()
    }
}
