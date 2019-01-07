package com.smallhacker.disbrowser

import com.smallhacker.disbrowser.asm.*
import com.smallhacker.disbrowser.disassembler.Disassembler
import com.smallhacker.disbrowser.util.tryParseInt
import java.nio.file.Paths
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

private val RESET_VECTOR_LOCATION = Address(0x00_FFFC)

@Path("/")
class MyResource {
    private val romData = lazy {
        //RomData.load(Paths.get("P:\\Emulation\\ROMs\\SNES\\Zelda no Densetsu - Kamigami no Triforce (Japan).sfc"))
        RomData.load(Paths.get("""P:\Emulation\ROMs\SNES\Super Mario World.sfc"""))
    }

    @GET
    fun getIt(): Response {
        return handle {
            val resetVectorLocation = RESET_VECTOR_LOCATION
            val initialAddress = Address(romData.value.getWord(resetVectorLocation.pc).value)
            val flags = parseState("MX")
            showDisassembly(initialAddress, flags)
        }
    }

    @GET
    @Path("{address}")
    fun getIt(@PathParam("address") address: String): Response {
        return handle {
            parseAddress(address)?.let {
                val flags = parseState("")
                showDisassembly(it, flags)
            }
        }
    }


    @GET
    @Path("{address}/{state}")
    @Produces(MediaType.TEXT_HTML)
    fun getIt(@PathParam("address") address: String, @PathParam("state") state: String): Response {
        return handle {
            parseAddress(address)?.let {
                val flags = parseState(state)
                showDisassembly(it, flags)
            }
        }
    }

    private fun parseAddress(address: String): Address? {
        return tryParseInt(address, 16)
                ?.let { Address(it) }
    }

    private fun handle(runner: () -> HtmlNode?): Response {
        try {
            val disassembly = runner()

            return if (disassembly == null)
                Response.status(404).build()
            else
                Response.ok(disassembly.toString(), MediaType.TEXT_HTML).build()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun showDisassembly(initialAddress: Address, flags: VagueNumber): HtmlNode? {
        val rom = romData.value

        val metadata = getMetadata()

        val initialState = State(data = rom, address = initialAddress, flags = flags)
        val disassembly = Disassembler.disassemble(initialState, metadata, false)

        return print(disassembly, metadata)
    }

    private fun parseState(state: String): VagueNumber {
        var flags = VagueNumber()
        flags = parseFlag(state, flags, 'M', 'm', 0x20)
        flags = parseFlag(state, flags, 'X', 'x', 0x10)
        return flags
    }

    @GET
    @Path("resources/{file}.{ext}")
    fun getCss(@PathParam("file") file: String, @PathParam("ext") ext: String): Response {
        val mime = when (ext) {
            "js" -> "application/javascript"
            "css" -> "text/css"
            else -> null
        }

        if (mime != null) {
            javaClass.getResourceAsStream("/$file.$ext")
                    ?.bufferedReader()
                    ?.use {
                        return Response.ok(it.readText())
                                .type(mime)
                                .build()
                    }
        }

        return Response.status(404).build()
    }


    private fun parseFlag(state: String, flags: VagueNumber, set: Char, clear: Char, mask: Int): VagueNumber = when {
        state.contains(set) -> flags.withBits(mask)
        state.contains(clear) -> flags.withoutBits(mask)
        else -> flags
    }

    private fun print(disassembly: Disassembly, metadata: Metadata): HtmlNode {
        val grid = Grid()
        disassembly.forEach { grid.add(it, metadata[it.address], disassembly) }
        disassembly.asSequence()
                .mapNotNull {
                    it.linkedState
                            ?.let { link ->
                                it.address to link.address
                            }
                }
                .sortedBy { Math.abs(it.first.value - it.second.value) }
                .forEach { grid.arrow(it.first, it.second) }

        return html {
            head {
                title { text("Disassembly Browser") }
                link {}.attr("rel", "stylesheet").attr("href", "/resources/style.css")
            }
            body {
                grid.output().appendTo(parent)
                script().attr("src", "/resources/main.js")
            }
        }
    }

    private fun getMetadata(): Metadata {
        return metadata {
            at(0x00_8000) { label = "RESET_VECTOR" }
            at(0x00_80c6) { flags.add(JmpIndirectLongInterleavedTable(Address(0x00_8061), 28)) }
            at(0x00_879c) { flags.add(NonReturningRoutine) }
            at(0x02_87d0) { flags.add(JslTableRoutine(4)) }
            at(0x0c_c115) { flags.add(JslTableRoutine(12)) }
            //at(0x0c_c115) { stop = true }
        }
    }
}

