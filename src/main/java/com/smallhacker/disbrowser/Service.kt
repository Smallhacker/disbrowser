package com.smallhacker.disbrowser

import com.smallhacker.disbrowser.asm.*
import com.smallhacker.disbrowser.disassembler.Disassembler
import com.smallhacker.disbrowser.util.jsonFile
import com.smallhacker.disbrowser.util.toUInt24
import java.nio.file.Paths
import kotlin.reflect.KMutableProperty1

private val RESET_VECTOR_LOCATION = address(0x00_FFFC)

private val VECTORS = listOf(
        address(0x00_FFE4) to "COP",
        address(0x00_FFE6) to "BRK",
        address(0x00_FFE8) to "ABORT",
        address(0x00_FFEA) to "NMI",
        address(0x00_FFEC) to "RESET",
        address(0x00_FFEE) to "IRQ",
        address(0x00_FFF4) to "COP (e)",
        address(0x00_FFF6) to "BRK (e)",
        address(0x00_FFF8) to "ABORT (e)",
        address(0x00_FFFA) to "NMI (e)",
        address(0x00_FFFC) to "RES (e)",
        address(0x00_FFFE) to "IRQBRK (e)"
)

object Service {
    private const val romName = "Zelda no Densetsu - Kamigami no Triforce (Japan)"
    private val romDir = Paths.get("""P:\Emulation\ROMs\SNES""")
    private val metaDir = Paths.get("""P:\Programming\dis-browser""")
    private val metaFile = jsonFile<Metadata>(metaDir.resolve("$romName.json"), true)
    private val metadata by lazy { metaFile.load() }

    private val snesMemory by lazy {
        val path = romDir.resolve("$romName.sfc")
        SnesLoRom(loadRomData(path))
    }

    fun showDisassemblyFromReset(): HtmlNode? {
        val resetVector = snesMemory.getWord(RESET_VECTOR_LOCATION)
        val fullResetVector = resetVector!!.toUInt24()
        val initialAddress = SnesAddress(fullResetVector)
        val flags = VagueNumber(0x30u)
        return showDisassembly(initialAddress, flags)
    }

    fun showDisassembly(initialAddress: SnesAddress, flags: VagueNumber): HtmlNode? {
        val initialState = State(memory = snesMemory, address = initialAddress, flags = flags, metadata = metadata)
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

        return grid.output()
    }

    fun updateMetadata(address: SnesAddress, field: KMutableProperty1<MetadataLine, String?>, value: String) {
        if (value.isEmpty()) {
            if (address in metadata) {
                doUpdateMetadata(address, field, null)
            }
        } else {
            doUpdateMetadata(address, field, value)
        }
    }

    private fun doUpdateMetadata(address: SnesAddress, field: KMutableProperty1<MetadataLine, String?>, value: String?) {
        val line = metadata.getOrCreate(address)
        field.set(line, value)

        metadata.cleanUp()
        metaFile.save(metadata)
    }

    fun getVectors() = VECTORS.asSequence()
            .map { (vectorLocation: SnesAddress, name: String ) ->
                val codeLocation = SnesAddress(snesMemory.getWord(vectorLocation)!!.toUInt24())
                val label = metadata[codeLocation]?.label
                        ?: codeLocation.toFormattedString()
                Vector(vectorLocation, codeLocation, name, label)
            }
}

data class Vector(val vectorLocation: SnesAddress, val codeLocation: SnesAddress, val name: String, val label: String)