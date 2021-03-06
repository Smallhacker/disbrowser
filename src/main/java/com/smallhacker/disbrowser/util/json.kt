package com.smallhacker.disbrowser.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Path

val jsonMapper: ObjectMapper by lazy {
    jacksonObjectMapper()
}

interface JsonFile<T> {
    fun load(): T
    fun save(value: T)
}

inline fun <reified T> jsonFile(path: Path, prettyPrint: Boolean = false): JsonFile<T> {
    val writer = if (prettyPrint) jsonMapper.writerWithDefaultPrettyPrinter() else jsonMapper.writer()
    return object : JsonFile<T> {
        override fun load() = jsonMapper.readValue<T>(path.toFile())
        override fun save(value: T) = writer.writeValue(path.toFile(), value)
    }
}