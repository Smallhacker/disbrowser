//package com.smallhacker.disbrowser
//
//class HtmlContext(val out: StringBuilder)
//
//fun html(inner: HtmlContext.() -> Unit = {}): String {
//    val html = HtmlContext(StringBuilder().append("<!DOCTYPE html>"))
//    element(html, "html") { inner(html) }
//    return html.out.toString()
//}
//
//private fun element(html: HtmlContext, tag: String, inner: HtmlContext.() -> Unit) = element(html, tag, inner, *emptyArray())
//
//private fun element(html: HtmlContext, tag: String, inner: HtmlContext.() -> Unit, vararg args: String) {
//    html.out.append("<$tag")
//    html.out.append(args.asSequence().map { " $it" }.joinToString())
//    html.out.append(">")
//    html.inner()
//    html.out.append("</$tag>")
//}
//
//
//fun HtmlContext.text(text: String) {
//    this.out.append(text)
//}
//
//fun HtmlContext.head(inner: HtmlContext.() -> Unit = {}) = element(this, "head", inner)
//fun HtmlContext.title(inner: HtmlContext.() -> Unit = {}) = element(this, "title", inner)
//fun HtmlContext.body(inner: HtmlContext.() -> Unit = {}) = element(this, "body", inner)
//fun HtmlContext.style(inner: HtmlContext.() -> Unit = {}) = element(this, "style", inner)
//fun HtmlContext.link(href: String, inner: HtmlContext.() -> Unit = {}) = element(this, "link", inner, "rel=\"stylesheet\" href=\"$href\"")
//fun HtmlContext.div(inner: HtmlContext.() -> Unit = {}) = element(this, "div", inner)
//fun HtmlContext.div(cssClass: String, inner: HtmlContext.() -> Unit = {}) = element(this, "div", inner, "class=\"$cssClass\"")
//
//fun HtmlContext.table(inner: HtmlContext.() -> Unit = {}) = element(this, "table", inner)
//fun HtmlContext.tr(inner: HtmlContext.() -> Unit = {}) = element(this, "tr", inner)
//fun HtmlContext.tr(cssClass: String?, inner: HtmlContext.() -> Unit = {}) = element(this, "tr", inner, (if (cssClass == null) "" else "class=\"$cssClass\""))
//fun HtmlContext.td(inner: HtmlContext.() -> Unit = {}) = element(this, "td", inner)
//fun HtmlContext.td(cssClass: String?, inner: HtmlContext.() -> Unit = {}) = element(this, "td", inner, (if (cssClass == null) "" else "class=\"$cssClass\""))
//fun HtmlContext.a(href: String, inner: HtmlContext.() -> Unit = {}) = element(this, "a", inner, "href=\"$href\"")