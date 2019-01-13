package com.smallhacker.disbrowser.resource

import com.smallhacker.disbrowser.*
import com.smallhacker.disbrowser.asm.SnesAddress
import com.smallhacker.disbrowser.asm.VagueNumber
import java.nio.charset.StandardCharsets
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
class DisassemblyResource {
    @GET
    @Produces(MediaType.TEXT_HTML)
    fun getIt() = handle {
        //Service.showDisassemblyFromReset()
        table {
            Service.getVectors().forEach {
                tr {
                    td { text(it.name) }
                    td { text("(" + it.vectorLocation.toFormattedString() + ")") }
                    td {
                        a {
                            text(it.label)
                        }.attr("href", "/${it.codeLocation.toSimpleString()}/MX")
                    }
                    td { text("(" + it.codeLocation.toFormattedString() + ")") }
                }
            }
        }.addClass("vector-table")
    }

    @GET
    @Path("{address}")
    @Produces(MediaType.TEXT_HTML)
    fun getIt(@PathParam("address") address: String) = getIt(address, "")

    @GET
    @Path("{address}/{state}")
    @Produces(MediaType.TEXT_HTML)
    fun getIt(@PathParam("address") address: String, @PathParam("state") state: String): Response {
        return handle {
            SnesAddress.parse(address)?.let {
                val flags = parseState(state)
                Service.showDisassembly(it, flags)
            }
        }
    }

    private fun handle(runner: () -> HtmlNode?): Response {
        try {
            val output = runner()
                    ?: return Response.status(404).build()

            val html = html {
                head {
                    title { text("Disassembly Browser") }
                    link {}.attr("rel", "stylesheet").attr("href", "/resources/style.css")
                    meta {}.attr("charset", "UTF-8")
                }
                body {
                    output.appendTo(parent)
                    script().attr("src", "/resources/disbrowser.js")
                }
            }

            return Response.ok(html.toString().toByteArray(StandardCharsets.UTF_8))
                    .encoding("UTF-8")
                    .build()
        } catch (e: Exception) {
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
