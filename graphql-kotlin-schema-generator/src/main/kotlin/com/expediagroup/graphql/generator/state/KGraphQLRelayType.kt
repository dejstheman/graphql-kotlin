package com.expediagroup.graphql.generator.state

import graphql.schema.GraphQLNamedType

data class KGraphQLRelayType(val connection: GraphQLNamedType, val edge: GraphQLNamedType)
