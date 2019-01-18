package com.smallhacker.disbrowser

import com.smallhacker.disbrowser.asm.*
import com.smallhacker.disbrowser.game.Game
import com.smallhacker.disbrowser.disassembler.Disassembler
import com.smallhacker.disbrowser.util.toUInt24
import kotlin.reflect.KMutableProperty1

private val RESET_VECTOR_LOCATION = address(0x00_FFFC)
private val UPDATE_MUTEX = Any()

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
    fun showDisassemblyFromReset(game: Game): HtmlNode? {
        val resetVector = game.memory.getWord(RESET_VECTOR_LOCATION)
        val fullResetVector = resetVector!!.toUInt24()
        val initialAddress = SnesAddress(fullResetVector)
        val flags = VagueNumber(0x30u)
        return showDisassembly(game, initialAddress, flags)
    }

    fun showDisassembly(game: Game, initialAddress: SnesAddress, flags: VagueNumber, global: Boolean = false): HtmlNode? {
        val initialState = State(memory = game.memory, address = initialAddress, flags = flags, gameData = game.gameData)
        val disassembly = Disassembler.disassemble(initialState, game.gameData, global)

        return print(disassembly, game)
    }

    private fun print(disassembly: Disassembly, game: Game): HtmlNode {
        val grid = Grid()
        disassembly.forEach {
            grid.add(it, game, disassembly)
        }
        disassembly.asSequence()
                .mapNotNull {ins ->
                    ins.linkedState
                            ?.let { link ->
                                ins.sortedAddress to link.address
                            }
                }
                .sortedBy { it.first distanceTo it.second }
                .forEach { grid.arrow(it.first, it.second) }

        return grid.output()
    }

    fun updateMetadata(game: Game, address: SnesAddress, field: KMutableProperty1<MetadataLine, String?>, value: String) {
        synchronized(UPDATE_MUTEX) {
            if (value.isEmpty()) {
                if (address in game.gameData) {
                    doUpdateMetadata(game, address, field, null)
                }
            } else {
                doUpdateMetadata(game, address, field, value)
            }
        }
    }

    private fun doUpdateMetadata(game: Game, address: SnesAddress, field: KMutableProperty1<MetadataLine, String?>, value: String?) {
        val line = game.gameData.getOrCreate(address)
        field.set(line, value)

        game.saveGameData()
    }

    fun getVectors(game: Game) = VECTORS.asSequence()
            .map { (vectorLocation: SnesAddress, name: String ) ->
                val codeLocation = SnesAddress(game.memory.getWord(vectorLocation)!!.toUInt24())
                val label = game.gameData[codeLocation]?.label
                        ?: codeLocation.toFormattedString()
                Vector(vectorLocation, codeLocation, name, label)
            }
}

data class Vector(val vectorLocation: SnesAddress, val codeLocation: SnesAddress, val name: String, val label: String)