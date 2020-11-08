package com.expediagroup.graphql.generator.types


import com.expediagroup.graphql.generator.state.KGraphQLRelayType
import graphql.Scalars
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLTypeReference
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

const val ConnectionTypeDescription = "A connection to a list of items."
const val EdgesFieldDescription = "A list of edges"
const val EdgeDescription = "An edge in a connection"
const val PageInfoFieldDescription = "Details about this specific page"
const val NodeFieldDescription = "The item at the end of the edge"
const val CursorFieldDescription = "Cursor marks a unique position or index into the connection"

internal fun generateRelayObjectTypes(type: KType): KGraphQLRelayType {
    val edgeType =
        GraphQLObjectType.newObject()
            .name(type.relayEdgeName())
            .description(EdgeDescription)
            .field(nodeField(type.relayTypePrefix()))
            .field(CursorField)
            .build()

    val connectionType =
        GraphQLObjectType.newObject()
            .name(type.relayConnectionName())
            .description(ConnectionTypeDescription)
            .field(edgeField(edgeType))
            .field(PageInfoField)
            .build()

    return KGraphQLRelayType(connectionType, edgeType)

}

internal fun KType.relayTypePrefix(): String =
    javaType.typeName.let {
        it.substring(it.lastIndexOf(".") + 1).removeSuffix(">")
    }

internal fun KType.relayConnectionName(): String = "${relayTypePrefix()}Connection"

internal fun KType.relayEdgeName(): String = "${relayTypePrefix()}Edge"

private fun edgeField(edgeType: GraphQLObjectType): GraphQLFieldDefinition.Builder =
    GraphQLFieldDefinition.newFieldDefinition()
        .name("edges")
        .description(EdgesFieldDescription)
        .type(GraphQLList.list(edgeType))

private fun nodeField(prefix: String): GraphQLFieldDefinition.Builder =
    GraphQLFieldDefinition.newFieldDefinition()
        .name("node")
        .type(GraphQLNonNull.nonNull(GraphQLTypeReference.typeRef(prefix)))
        .description(NodeFieldDescription)

private val PageInfoField =
    GraphQLFieldDefinition.newFieldDefinition()
        .name("pageInfo")
        .description(PageInfoFieldDescription)
        .type(GraphQLNonNull.nonNull(GraphQLTypeReference.typeRef("PageInfo")))

private val CursorField: GraphQLFieldDefinition.Builder =
    GraphQLFieldDefinition.newFieldDefinition()
        .name("cursor")
        .type(GraphQLNonNull.nonNull(Scalars.GraphQLString))
        .description(CursorFieldDescription)
