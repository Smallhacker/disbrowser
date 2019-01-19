package com.smallhacker.disbrowser

import com.smallhacker.disbrowser.asm.*
import com.smallhacker.disbrowser.game.Game

class Grid {
    private val arrowCells = HashMap<Pair<Int, Int>, HtmlNode?>()
    private val arrowClasses = HashMap<Pair<Int, Int>, String>()
    private var arrowWidth = 0

    private val content = HashMap<Pair<Int, Int>, HtmlNode?>()
    private val cellClasses = HashMap<Pair<Int, Int>, String>()
    private val addresses = HashMap<SnesAddress, Int>()
    private val rowClasses = HashMap<Int, String>()
    private val rowCertainties = HashMap<Int, String>()
    private val rowId = HashMap<Int, String>()
    private var height = 0
    private var nextAddress: SnesAddress? = null

    fun arrow(from: SnesAddress, to: SnesAddress) {
        val yStart = addresses[from]
        val yEnd = addresses[to]
        if (yStart == null || yEnd == null) {
            return
        }

        val y1: Int
        val y2: Int
        val dir: String
        if (yStart > yEnd) {
            dir = "up"
            y1 = yEnd
            y2 = yStart
        } else {
            dir = "down"
            y1 = yStart
            y2 = yEnd
        }

        val x = nextArrowX(y1, y2)
        if ((x + 1) > arrowWidth) {
            arrowWidth = x + 1
        }

        arrowClasses[x to y1] = "arrow arrow-$dir-start"
        for (y in (y1 + 1)..(y2 - 1)) {
            arrowClasses[x to y] = "arrow arrow-$dir-middle"
        }
        arrowClasses[x to y2] = "arrow arrow-$dir-end"
        arrowCells[x to yEnd] = htmlFragment {
            div.addClass("arrow-head")
        }
    }

    private fun nextArrowX(y1: Int, y2: Int): Int {
        return generateSequence(0) { it + 1 }
                .filter { x ->
                    (y1..y2).asSequence()
                            .map { y -> arrowClasses[x to y] }
                            .all { it == null }
                }
                .first()
    }

    fun add(ins: CodeUnit, game: Game, disassembly: Disassembly) {
        val sortedAddress = ins.sortedAddress
        val indicativeAddress = ins.indicativeAddress
        val gameData = game.gameData

        if (nextAddress != null) {
            if (sortedAddress != nextAddress) {
                addDummy()
            }
        }
        nextAddress = ins.nextSortedAddress

        val y = (height++)
        addresses[sortedAddress] = y

        val (address, bytes, label, primaryMnemonic, secondaryMnemonic, suffix, operands, state, comment, labelAddress)
                = ins.print(gameData)

        add(y, ins.address,
            htmlFragment {
                text(address ?: "")
            },
            htmlFragment {
                text(bytes)
            },
                editableField(game, indicativeAddress, "label", label),
            htmlFragment {
                if (secondaryMnemonic == null) {
                    text(primaryMnemonic)
                } else {
                    span {
                        text(primaryMnemonic)
                    }.attr("title", secondaryMnemonic).addClass("opcode-info")
                }
                text(suffix ?: "")
                text(" ")
                val link = ins.linkedState
                if (link == null) {
                    if (labelAddress == null) {
                        text(operands ?: "")
                    } else {
                        val currentLabel = gameData[labelAddress]?.label
                        editablePopupField(game, labelAddress, "label", operands, currentLabel)
                    }
                } else {
                    val local = link.address in disassembly

                    val url = when {
                        local -> "#${link.address.toSimpleString()}"
                        else -> "/${game.id}/${link.address.toSimpleString()}/${link.urlString}"
                    }

                    a {
                        text(operands ?: "")
                    }.attr("href", url)
                }
            },
            htmlFragment {
                text(state ?: "")
            },
                editableField(game, indicativeAddress, "comment", comment)
        )

        if (ins.opcode.continuation.shouldStop) {
            rowClasses[y] = "routine-end"
        }

        rowCertainties[y] = ins.certainty.value.toString()
    }

    private fun editableField(game: Game, address: SnesAddress, type: String, value: String?): HtmlNode {
        return htmlFragment {
            input.attr("value", value ?: "")
                .attr("type", "text")
                .addClass("field-$type")
                .addClass("field-editable")
                .attr("data-field", type)
                .attr("data-game", game.id)
                .attr("data-address", address.toSimpleString())
        }
    }

    private fun HtmlArea.editablePopupField(game: Game, address: SnesAddress, type: String, displayValue: String?, editValue: String?) {
        span {
            text(displayValue ?: "")
            span {}.addClass("field-editable-popup-icon")
        }
                .addClass("field-$type")
                .addClass("field-editable-popup")
                .attr("data-field", type)
                .attr("data-value", editValue ?: "")
                .attr("data-game", game.id)
                .attr("data-address", address.toSimpleString())
    }

    private fun addDummy() {
        val y = (height++)
        add(y,
                null,
                null,
                null,
                null,
            htmlFragment {
                text("...")
            },
                null,
                null
        )
    }

    private fun add(y: Int, address: SnesAddress?,
                    cAddress: HtmlNode?,
                    cBytes: HtmlNode?,
                    cLabel: HtmlNode?,
                    cCode: HtmlNode?,
                    cState: HtmlNode?,
                    cComment: HtmlNode?
    ) {
        if (address != null) {
            rowId[y] = address.toSimpleString()
        }
        content[0 to y] = cAddress
        content[1 to y] = cBytes
        content[2 to y] = cLabel
        content[3 to y] = cCode
        content[4 to y] = cState
        content[5 to y] = cComment
    }

    fun output(): HtmlNode {
        val contentMaxX = content.keys.asSequence()
                .map { it.first }
                .max()
                ?: -1

        return htmlFragment {
            table {
                for (y in 0 until height) {
                    tr {
                        for (x in 0..3) {
                            val cssClass = cellClasses[x to y]
                            td {
                                content[x to y]?.appendTo(parent)
                            }.addClass(cssClass)
                        }
                        for (x in 0 until arrowWidth) {
                            val cssClass = arrowClasses[x to y]
                            td {
                                arrowCells[x to y]?.appendTo(parent)
                            }.addClass(cssClass)
                        }
                        for (x in 4..contentMaxX) {
                            val cssClass = cellClasses[x to y]
                            td {
                                content[x to y]?.appendTo(parent)
                            }.addClass(cssClass)
                        }
                    }.addClass(rowClasses[y])
                        .attr("id", rowId[y])
                        .attr("row-certainty", rowCertainties[y])
                }
            }
        }
    }
}
