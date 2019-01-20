package com.smallhacker.disbrowser.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule

typealias KSerializer<T> = T.(gen: JsonGenerator, serializers: SerializerProvider) -> Unit
typealias KDeserializer<T> = JsonParser.(ctxt: DeserializationContext) -> T
typealias KKeyDeserializer<T> = String.(ctxt: DeserializationContext) -> T

interface JsonTypeMapper<T> {
    val type: Class<T>
    fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider)
    fun deserialize(p: JsonParser, ctxt: DeserializationContext): T
    fun keyDeserialize(value: String, ctxt: DeserializationContext): T
}

inline fun <reified T> jsonTypeMapper(
    noinline serializer: KSerializer<T>,
    noinline deserializer: KDeserializer<T>,
    noinline keyDeserializer: KKeyDeserializer<T>
) =
    object : JsonTypeMapper<T> {
        override val type: Class<T> = T::class.java

        override fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider) =
            serializer(value, gen, serializers)

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext) =
            deserializer(p, ctxt)

        override fun keyDeserialize(value: String, ctxt: DeserializationContext) =
            keyDeserializer(value, ctxt)
    }

inline fun <reified T : Any> ObjectMapper.addMapper(
    noinline serializer: KSerializer<T>,
    noinline deserializer: KDeserializer<T>,
    noinline keyDeserializer: KKeyDeserializer<T>
) = addMapper(jsonTypeMapper(serializer, deserializer, keyDeserializer))

fun <T : Any> ObjectMapper.addMapper(mapper: JsonTypeMapper<T>) =
    registerModule(object : SimpleModule() {
        init {
            val serializer = object : JsonSerializer<T>() {
                override fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider) =
                    mapper.serialize(value, gen, serializers)
            }
            val deserializer = object : JsonDeserializer<T>() {
                override fun deserialize(p: JsonParser, ctxt: DeserializationContext) =
                    mapper.deserialize(p, ctxt)
            }
            val keyDeserializer = object : KeyDeserializer() {
                override fun deserializeKey(key: String, ctxt: DeserializationContext) =
                    mapper.keyDeserialize(key, ctxt)

            }
            addSerializer(mapper.type, serializer)
            addDeserializer(mapper.type, deserializer)
            addKeySerializer(mapper.type, serializer)
            addKeyDeserializer(mapper.type, keyDeserializer)
        }
    })!!