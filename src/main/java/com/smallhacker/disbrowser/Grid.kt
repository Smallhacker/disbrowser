package com.smallhacker.disbrowser

import com.smallhacker.disbrowser.asm.*

class Grid {
    private val arrowCells = HashMap<Pair<Int, Int>, HtmlNode?>()
    private val arrowClasses = HashMap<Pair<Int, Int>, String>()
    private var arrowWidth = 0

    private val content = HashMap<Pair<Int, Int>, HtmlNode?>()
    private val cellClasses = HashMap<Pair<Int, Int>, String>()
    private val addresses = HashMap<Address, Int>()
    private val rowClasses = HashMap<Int, String>()
    private val rowId = HashMap<Int, String>()
    private var height = 0
    private var nextAddress: Address? = null

    fun arrow(from: Address, to: Address) {
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
        //arrowCells[x to yStart] = a().addClass("arrow-link").attr("href", "#${to.toSimpleString()}")
        arrowCells[x to yEnd] = div().addClass("arrow-head")
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

    fun add(ins: Instruction, metadata: MetadataLine?, disassembly: Disassembly) {
        val address = ins.address

        if (nextAddress != null) {
            if (address != nextAddress) {
                addDummy()
            }
        }
        nextAddress = ins.postState.address

        val y = (height++)
        addresses[address] = y

        add(y, ins.address,
                text(ins.address.toFormattedString()),
                text(ins.bytesToString()),
                text(metadata?.label?.plus(":") ?: ""),
                fragment {
                    text("${ins.opcode.mnemonic}")
                    text(ins.lengthSuffix)
                    text(" ")
                    val operands = ins.opcode.mode.print(ins)

                    val link = ins.linkedState
                    if (link == null) {
                        text(operands)
                    } else {
                        val local = link.address in disassembly

                        val url = when {
                            local -> "#${link.address.toSimpleString()}"
                            else -> "/${link.address.toSimpleString()}/${link.urlString}"
                        }


                        a {
                            text(operands)
                        }.attr("href", url)
                    }
                },
                text(ins.postState.toString())
        )

        if (ins.opcode.continuation == Continuation.NO) {
            rowClasses[y] = "routine-end"
        }
    }

    private fun addDummy() {
        val y = (height++)
        add(y, null, null, null, null, text("..."), null)
    }

    private fun add(y: Int, address: Address?,
                    cAddress: HtmlNode?,
                    cBytes: HtmlNode?,
                    cLabel: HtmlNode?,
                    cCode: HtmlNode?,
                    cState: HtmlNode?
    ) {
        if (address != null) {
            rowId[y] = address.toSimpleString()
        }
        content[0 to y] = cAddress
        content[1 to y] = cBytes
        content[2 to y] = cLabel
        content[3 to y] = cCode
        content[4 to y] = cState
    }

    fun output(): HtmlNode {
        return table {
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
                    for (x in 4..4) {
                        val cssClass = cellClasses[x to y]
                        td {
                            content[x to y]?.appendTo(parent)
                        }.addClass(cssClass)
                    }
                }.addClass(rowClasses[y]).attr("id", rowId[y])
            }
        }
    }
}
