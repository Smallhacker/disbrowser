package com.smallhacker.disbrowser

import com.smallhacker.disbrowser.asm.*
import com.smallhacker.disbrowser.disassembler.Disassembler
import com.smallhacker.disbrowser.util.jsonFile
import com.smallhacker.disbrowser.util.toUInt24
import java.nio.file.Paths
import kotlin.reflect.KMutableProperty1

private val RESET_VECTOR_LOCATION = address(0x00_FFFC)

object Service {
    private val romName = "Zelda no Densetsu - Kamigami no Triforce (Japan)"
    private val romDir = Paths.get("""P:\Emulation\ROMs\SNES""")
    private val metaDir = Paths.get("""P:\Programming\dis-browser""")
    private val metaFile = jsonFile<Metadata>(metaDir.resolve("$romName.json"))
    private val metadata by lazy { metaFile.load() }

    private val romData = lazy {
        val path = romDir.resolve("$romName.sfc")
        RomData.load(path)
    }

    fun showDisassemblyFromReset(): HtmlNode? {
        val resetVectorLocation = RESET_VECTOR_LOCATION
        val initialAddress = Address(romData.value.getWord(resetVectorLocation.pc).toUInt24())
        val flags = VagueNumber(0x30u)
        return showDisassembly(initialAddress, flags)
    }

    fun showDisassembly(initialAddress: Address, flags: VagueNumber): HtmlNode? {
        val rom = romData.value

        val initialState = State(data = rom, address = initialAddress, flags = flags)
        val disassembly = Disassembler.disassemble(initialState, metadata, false)

        return print(disassembly, metadata)
    }


    private fun print(disassembly: Disassembly, metadata: Metadata): HtmlNode {
        val grid = Grid()
        disassembly.forEach {
            grid.add(it, metadata, disassembly)
        }
        disassembly.asSequence()
                .mapNotNull {
                    it.linkedState
                            ?.let { link ->
                                it.presentedAddress to link.address
                            }
                }
                .sortedBy { it.first distanceTo it.second }
                .forEach { grid.arrow(it.first, it.second) }

        return html {
            head {
                title { text("Disassembly Browser") }
                link {}.attr("rel", "stylesheet").attr("href", "/resources/style.css")
            }
            body {
                grid.output().appendTo(parent)
                script().attr("src", "/resources/disbrowser.js")
            }
        }
    }

    fun updateMetadata(address: Address, field: KMutableProperty1<MetadataLine, String?>, value: String) {
        if (value.isEmpty()) {
            metadata[address]?.run {
                field.set(this, null)
            }
        } else {
            field.set(metadata.getOrAdd(address), value)
        }
    }

}
