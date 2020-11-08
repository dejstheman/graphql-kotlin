package com.expediagroup.graphql.generator.state

import com.expediagroup.graphql.generator.SchemaGenerator
import com.expediagroup.graphql.generator.extensions.getKClass
import com.expediagroup.graphql.generator.extensions.isConnectionType
import com.expediagroup.graphql.generator.extensions.isEdgeType
import com.expediagroup.graphql.generator.types.generateRelayObjectTypes
import com.expediagroup.graphql.generator.types.relayTypePrefix
import graphql.relay.Connection
import graphql.relay.Edge
import graphql.schema.GraphQLType
import java.io.Closeable
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal class RelayTypesCache : Closeable {

    private val cache = mutableMapOf<String, KGraphQLRelayType>()

    internal fun get(cacheKey: TypesCacheKey): KGraphQLRelayType? =
        getCacheKeyString(cacheKey)?.let { cache[it] }

    internal fun put(key: TypesCacheKey, kGraphQLRelayType: KGraphQLRelayType): KGraphQLRelayType? =
        getCacheKeyString(key)?.let { cacheKeyString ->
            cache[cacheKeyString] = kGraphQLRelayType
            kGraphQLRelayType
        }

    internal fun build(type: KType, generator: SchemaGenerator, fallBackFn: (KClass<*>) -> GraphQLType): GraphQLType {
        val cacheKey = TypesCacheKey(type, false)
        return when (val cachedType = get(cacheKey)) {
            null -> updateCache(type, cacheKey, generator, fallBackFn)
            else -> when (type.classifier) {
                Connection::class -> cachedType.connection
                Edge::class -> cachedType.edge
                else -> fallBackFn(type.getKClass())
            }
        }
    }

    private fun updateCache(type: KType, cacheKey: TypesCacheKey, generator: SchemaGenerator, fallBackFn: (KClass<*>) -> GraphQLType): GraphQLType =
        when (type.classifier) {
            Connection::class, Edge::class -> {
                val (connection, edge) = generateRelayObjectTypes(type).let { Pair(it.connection, it.edge) }
                generator.config.hooks.willAddGraphQLTypeToSchema(type, connection)
                generator.config.hooks.willAddGraphQLTypeToSchema(type, edge)
                put(cacheKey, KGraphQLRelayType(connection, edge))
                when (type.classifier) {
                    Connection::class -> connection
                    Edge::class -> edge
                    else -> fallBackFn(type.getKClass())
                }
            }
            else -> fallBackFn(type.getKClass())
        }


    override fun close(): Unit {
        cache.clear()
    }

    private fun getCacheKeyString(cacheKey: TypesCacheKey): String? {
        val type = cacheKey.type
        val kClass = type.getKClass()

        return when {
            kClass.isConnectionType() || kClass.isEdgeType() -> type.relayTypePrefix()
            else -> null
        }
    }
}
