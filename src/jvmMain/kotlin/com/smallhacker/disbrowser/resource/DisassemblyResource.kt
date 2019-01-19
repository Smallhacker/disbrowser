package com.smallhacker.disbrowser.resource

import com.smallhacker.disbrowser.*
import com.smallhacker.disbrowser.asm.SnesAddress
import com.smallhacker.disbrowser.asm.VagueNumber
import com.smallhacker.disbrowser.game.getGameSource
import java.nio.charset.StandardCharsets
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Configuration
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/{game}")
class DisassemblyResource {
    @Context
    private lateinit var config: Configuration
    private val games by lazy { config.getGameSource() }

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun getIt(@PathParam("game") gameName: String) = handle {
        games.getGame(gameName)?.let { game ->
            htmlFragment {
                table {
                    Service.getVectors(game).forEach {
                        tr {
                            td { text(it.name) }
                            td { text("(" + it.vectorLocation.toFormattedString() + ")") }
                            td {
                                a {
                                    text(it.label)
                                }.attr("href", "/${game.id}/${it.codeLocation.toSimpleString()}/MX")
                            }
                            td { text("(" + it.codeLocation.toFormattedString() + ")") }
                        }
                    }
                }.addClass("vector-table")
            }
        }
    }

    @GET
    @Path("{address}")
    @Produces(MediaType.TEXT_HTML)
    fun getIt(
            @PathParam("game") gameName: String,
            @PathParam("address") address: String
    ) = getIt(gameName, address, "")

    @GET
    @Path("{address}/{state}")
    @Produces(MediaType.TEXT_HTML)
    fun getIt(
            @PathParam("game") gameName: String,
            @PathParam("address") address: String,
            @PathParam("state") state: String
    ): Response {
        return handle {
            games.getGame(gameName)?.let { game ->
                SnesAddress.parse(address)?.let {
                    val flags = parseState(state)
                    Service.showDisassembly(game, it, flags)
                }
            }
        }
    }

    @GET
    @Path("global/{address}")
    @Produces(MediaType.TEXT_HTML)
    fun getItGlobal(
            @PathParam("game") gameName: String,
            @PathParam("address") address: String
    ) = getItGlobal(gameName, address, "")

    @GET
    @Path("global/{address}/{state}")
    @Produces(MediaType.TEXT_HTML)
    fun getItGlobal(
            @PathParam("game") gameName: String,
            @PathParam("address") address: String,
            @PathParam("state") state: String
    ): Response {
        return handle {
            games.getGame(gameName)?.let { game ->
                SnesAddress.parse(address)?.let {
                    val flags = parseState(state)
                    Service.showDisassembly(game, it, flags, true)
                }
            }
        }
    }

    private fun handle(runner: () -> HtmlNode?): Response {
        try {
            val output = runner()
                    ?: return Response.status(404).build()

            val html =
                htmlFragment {
                    html {
                        head {
                            title { text("Disassembly Browser") }
                            link.attr("rel", "stylesheet").attr("href", "/resources/style.css")
                            meta.attr("charset", "UTF-8")
                        }
                        body {
                            main {
                                output.appendTo(parent)
                            }

                            aside.addClass("sidebar") {
                                button.attr("id", "btn-dark-mode") {
                                    text("Dark Mode")
                                }
                            }

                            script.attr("src", "/resources/disbrowser.js")
                        }
                    }
                }

            return Response.ok(html.toString().toByteArray(StandardCharsets.UTF_8))
                    .encoding("UTF-8")
                    .build()
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    private fun parseState(state: String): VagueNumber {
        var flags = VagueNumber()
        flags = parseFlag(state, flags, 'M', 'm', 0x20u)
        flags = parseFlag(state, flags, 'X', 'x', 0x10u)
        return flags
    }

    private fun parseFlag(state: String, flags: VagueNumber, set: Char, clear: Char, mask: UInt): VagueNumber = when {
        state.contains(set) -> flags.withBits(mask)
        state.contains(clear) -> flags.withoutBits(mask)
        else -> flags
    }
}
