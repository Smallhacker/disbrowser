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
    private const val BASE_URI = "http://localhost:8080/"

    private fun startServer(path: Path): HttpServer {
        val rc = ResourceConfig()
                .packages("com.smallhacker.disbrowser.resource")
                .addGameSource(path)
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc)
    }

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 1) {
            throw IllegalArgumentException("Game data directory needed")
        }
        val server = startServer(Paths.get(args[0]))
        println("Server started. Press any key to stop.")
        System.`in`.read()
        server.stop()
    }
}

