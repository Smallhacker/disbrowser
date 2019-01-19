package com.smallhacker.disbrowser

import kotlin.reflect.KProperty

typealias InnerHtml = HtmlArea.() -> Unit

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

private class ParentHtmlElement(tag: String) : HtmlElement(tag) {
    private val children = ArrayList<HtmlNode>()

    override fun printTo(out: StringBuilder): StringBuilder {
        super.printTo(out)
        children.forEach { it.printTo(out) }
        out.append("</", tag, ">")
        return out
    }

    override fun append(node: HtmlNode) = apply { children.add(node) }
}

class HtmlArea(val parent: HtmlNode)

private class HtmlTextNode(private val text: String): HtmlNode {
    override fun printTo(out: StringBuilder) = out.append(text)
    override fun attr(key: String, value: String?) = this
    override fun append(node: HtmlNode): HtmlNode = this
}

private object ParentBuilder {
    operator fun getValue(a: HtmlArea, b: KProperty<*>) = ParentHtmlElement(
        b.name
    ).appendTo(a.parent)
}
private object LeafBuilder {
    operator fun getValue(a: HtmlArea, b: KProperty<*>) = HtmlElement(
        b.name
    ).appendTo(a.parent)
}

fun htmlFragment(inner: InnerHtml = {}) = object : HtmlNode {
    private val children = ArrayList<HtmlNode>()

    init {
        inner(HtmlArea(this))
    }

    override fun printTo(out: StringBuilder) = out.apply { children.forEach { it.printTo(out) } }

    override fun append(node: HtmlNode) = apply { children.add(node) }

    override fun toString(): String = print()
}
fun HtmlArea.text(text: String) = HtmlTextNode(text).appendTo(parent)
val HtmlArea.fragment get() = htmlFragment().appendTo(parent)
val HtmlArea.html by ParentBuilder
val HtmlArea.head by ParentBuilder
val HtmlArea.title by ParentBuilder
val HtmlArea.link by LeafBuilder
val HtmlArea.meta by LeafBuilder
val HtmlArea.body by ParentBuilder
val HtmlArea.main by ParentBuilder
val HtmlArea.aside by ParentBuilder
val HtmlArea.div by ParentBuilder
val HtmlArea.span by ParentBuilder
val HtmlArea.table by ParentBuilder
val HtmlArea.tr by ParentBuilder
val HtmlArea.td by ParentBuilder
val HtmlArea.a by ParentBuilder
val HtmlArea.script by ParentBuilder
val HtmlArea.input by LeafBuilder
val HtmlArea.button by ParentBuilder

fun HtmlNode.appendTo(node: HtmlNode) = apply { node.append(this) }
fun HtmlNode.addClass(c: String?) = attrAdd("class", c)
val HtmlNode.inner get() = this
operator fun HtmlNode.invoke(inner: InnerHtml): HtmlNode = append(
    htmlFragment(inner)
)

fun HtmlNode.attr(key: String, value: String?, inner: InnerHtml) = attr(key, value).inner(inner)
fun HtmlNode.addClass(c: String?, inner: InnerHtml) = addClass(c).inner(inner)
fun HtmlNode.append(node: HtmlNode, inner: InnerHtml) = append(node).inner(inner)

private fun HtmlNode.attrSet(name: String): MutableSet<String> = (attr(name) ?: "")
        .split(" ")
        .asSequence()
        .filterNot { it.isEmpty() }
        .toMutableSet()

private fun HtmlNode.attrAdd(name: String, value: String?) = apply {
    if (value != null) {
        val set = attrSet(name)
        set.add(value)
        attr(name, set.joinToString(" "))
    }
}