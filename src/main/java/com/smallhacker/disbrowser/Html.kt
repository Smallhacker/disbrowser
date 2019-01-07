package com.smallhacker.disbrowser

interface HtmlNode {
    fun print(): String {
        val out = StringBuilder()
        printTo(out)
        return out.toString()
    }

    fun printTo(out: StringBuilder): StringBuilder

    fun attr(key: String): String? = null

    fun attr(key: String, value: String?): HtmlNode = this

    fun append(node: HtmlNode): HtmlNode = this
}

open class HtmlElement(protected val tag: String) : HtmlNode {
    private val attributes = LinkedHashMap<String, String>()

    final override fun toString(): String = print()

    override fun printTo(out: StringBuilder): StringBuilder {
        out.append("<", tag)
        attributes.forEach { key, value ->
            out.append(" ", key, "=\"", value, "\"")
        }
        return out.append(">")
    }

    final override fun attr(key: String): String? = attributes[key]

    final override fun attr(key: String, value: String?) = apply {
        if (value != null) {
            attributes[key] = value
        } else {
            attributes.remove("key")
        }
    }
}

private class ParentHtmlElement(tag: String, inner: InnerHtml) : HtmlElement(tag) {
    private val children = ArrayList<HtmlNode>()

    init {
        inner(HtmlArea(this))
    }

    override fun printTo(out: StringBuilder): StringBuilder {
        super.printTo(out)
        children.forEach { it.printTo(out) }
        out.append("</", tag, ">")
        return out
    }

    override fun append(node: HtmlNode) = apply { children.add(node) }
}

private fun parent(tag: String, inner: InnerHtml): HtmlNode = ParentHtmlElement(tag, inner)
private fun leaf(tag: String): HtmlNode = HtmlElement(tag)

typealias InnerHtml = HtmlArea.() -> Unit

class HtmlArea(val parent: HtmlNode)

fun text(text: String): HtmlNode = object : HtmlNode {
    override fun printTo(out: StringBuilder) = out.append(text)
    override fun attr(key: String, value: String?) = this
    override fun append(node: HtmlNode): HtmlNode = this
}

fun HtmlArea.text(text: String) = com.smallhacker.disbrowser.text(text).appendTo(parent)

fun fragment(inner: InnerHtml = {}) = object : HtmlNode {
    private val children = ArrayList<HtmlNode>()

    init {
        inner(HtmlArea(this))
    }

    override fun printTo(out: StringBuilder) = out.apply { children.forEach { it.printTo(out) } }

    override fun append(node: HtmlNode) = apply { children.add(node) }
}

fun HtmlArea.fragment(inner: InnerHtml = {}) = com.smallhacker.disbrowser.fragment(inner).appendTo(parent)
fun html(inner: InnerHtml = {}) = parent("html", inner)
fun HtmlArea.html(inner: InnerHtml = {}) = com.smallhacker.disbrowser.html(inner).appendTo(parent)
fun head(inner: InnerHtml = {}) = parent("head", inner)
fun HtmlArea.head(inner: InnerHtml = {}) = com.smallhacker.disbrowser.head(inner).appendTo(parent)
fun title(inner: InnerHtml = {}) = parent("title", inner)
fun HtmlArea.title(inner: InnerHtml = {}) = com.smallhacker.disbrowser.title(inner).appendTo(parent)
fun link(inner: InnerHtml = {}) = leaf("link")
fun HtmlArea.link(inner: InnerHtml = {}) = com.smallhacker.disbrowser.link(inner).appendTo(parent)
fun body(inner: InnerHtml = {}) = parent("body", inner)
fun HtmlArea.body(inner: InnerHtml = {}) = com.smallhacker.disbrowser.body(inner).appendTo(parent)
fun div(inner: InnerHtml = {}) = parent("div", inner)
fun HtmlArea.div(inner: InnerHtml = {}) = com.smallhacker.disbrowser.div(inner).appendTo(parent)
fun table(inner: InnerHtml = {}) = parent("table", inner)
fun HtmlArea.table(inner: InnerHtml = {}) = com.smallhacker.disbrowser.table(inner).appendTo(parent)
fun tr(inner: InnerHtml = {}) = parent("tr", inner)
fun HtmlArea.tr(inner: InnerHtml = {}) = com.smallhacker.disbrowser.tr(inner).appendTo(parent)
fun td(inner: InnerHtml = {}) = parent("td", inner)
fun HtmlArea.td(inner: InnerHtml = {}) = com.smallhacker.disbrowser.td(inner).appendTo(parent)
fun a(inner: InnerHtml = {}) = parent("a", inner)
fun HtmlArea.a(inner: InnerHtml = {}) = com.smallhacker.disbrowser.a(inner).appendTo(parent)
fun script(inner: InnerHtml = {}) = parent("script", inner)
fun HtmlArea.script(inner: InnerHtml = {}) = com.smallhacker.disbrowser.script(inner).appendTo(parent)

fun HtmlNode.appendTo(node: HtmlNode) = apply { node.append(this) }
fun HtmlNode.addClass(c: String?) = attrAdd("class", c)

fun HtmlNode.attrSet(name: String): MutableSet<String> = (attr(name) ?: "")
        .split(" ")
        .asSequence()
        .filterNot { it.isEmpty() }
        .toMutableSet()

fun HtmlNode.attrAdd(name: String, value: String?) = apply {
    if (value != null) {
        val set = attrSet(name)
        set.add(value)
        attr(name, set.joinToString(" "))
    }
}