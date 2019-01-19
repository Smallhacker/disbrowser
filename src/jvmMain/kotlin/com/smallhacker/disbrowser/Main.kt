package com.smallhacker.disbrowser

import com.smallhacker.disbrowser.game.addGameSource
import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.glassfish.jersey.server.ResourceConfig

import java.io.IOException
import java.lang.IllegalArgumentException
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

object Main {
    private fun startServer(uri: URI, path: Path): HttpServer {
        val rc = ResourceConfig()
                .packages("com.smallhacker.disbrowser.resource")
                .addGameSource(path)
        return GrizzlyHttpServerFactory.createHttpServer(uri, rc)
    }

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 2) {
            throw IllegalArgumentException("Usage: \"http://websitelocation:1234\" \"/path/to/game/data/directory\"")
        }
        val uri = URI.create(args[0])
        val dir = Paths.get(args[1])
        val server = startServer(uri, dir)
        println("Server started. Press any key to stop.")
        System.`in`.read()
        server.stop()
    }
}

