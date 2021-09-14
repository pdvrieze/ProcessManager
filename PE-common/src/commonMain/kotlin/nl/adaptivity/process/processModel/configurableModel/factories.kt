/*
 * Copyright (c) 2019.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel.configurableModel

import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xmlutil.Namespace

val ConfigurableNodeContainer<*>.startNode
    get():StartNode.Builder = StartNodeBase.Builder()

fun ConfigurableNodeContainer<*>.startNode(config: @ConfigurationDsl StartNode.Builder.() -> Unit): StartNode.Builder =
    StartNodeBase.Builder().apply(config)


fun ConfigurableNodeContainer<*>.activity(predecessor: Identified): MessageActivity.Builder =
    MessageActivityBase.Builder().apply {
        this.predecessor = predecessor
    }

fun ConfigurableNodeContainer<*>.activity(
    predecessor: Identified,
    config: @ConfigurationDsl MessageActivity.Builder.() -> Unit
): MessageActivity.Builder = activity(predecessor).apply(config)

fun ConfigurableNodeContainer<*>.compositeActivity(predecessor: Identified): CompositeActivity.ModelBuilder =
    ActivityBase.CompositeActivityBuilder(
        configurationBuilder.rootBuilder,
        predecessor = predecessor
    )

fun ConfigurableNodeContainer<*>.compositeActivity(
    predecessor: Identified,
    config: @ConfigurationDsl CompositeActivity.ModelBuilder.() -> Unit
): CompositeActivity.ModelBuilder =
    ActivityBase.CompositeActivityBuilder(
        configurationBuilder.rootBuilder, predecessor = predecessor
    ).apply(config)

@ConfigurationDsl
fun ConfigurableNodeContainer<*>.split(predecessor: Identified): Split.Builder =
    SplitBase.Builder().apply { this.predecessor = predecessor }

@ConfigurationDsl
fun ConfigurableNodeContainer<*>.split(
    predecessor: Identified,
    config: @ConfigurationDsl Split.Builder.() -> Unit
): Split.Builder =
    split(predecessor).apply(config)

@ConfigurationDsl
fun ConfigurableNodeContainer<*>.join(vararg predecessors: Identified): Join.Builder = JoinBase.Builder().apply {
    this.predecessors = IdentifyableSet.processNodeSet(predecessors)
}

@ConfigurationDsl
fun ConfigurableNodeContainer<*>.join(predecessors: Collection<Identified>): Join.Builder = JoinBase.Builder().apply {
    this.predecessors = IdentifyableSet.processNodeSet(predecessors)
}

@ConfigurationDsl
fun ConfigurableNodeContainer<*>.join(
    vararg predecessors: Identified,
    config: @ConfigurationDsl Join.Builder.() -> Unit
): Join.Builder = join(*predecessors).apply(config)

@ConfigurationDsl
fun ConfigurableNodeContainer<*>.join(
    predecessors: Collection<Identified>,
    config: @ConfigurationDsl Join.Builder.() -> Unit
): Join.Builder =join(predecessors).apply(config)

fun ConfigurableNodeContainer<*>.endNode(predecessor: Identified): EndNode.Builder =
    EndNodeBase.Builder().apply {
        this.predecessor = predecessor
    }

fun ConfigurableNodeContainer<*>.endNode(
    predecessor: Identified,
    config: @ConfigurationDsl EndNode.Builder.() -> Unit
): EndNode.Builder = endNode(predecessor).apply(config)


fun ConfigurableProcessModel<*>.input(
    name: String,
    refNode: Identified,
    refName: String? = null,
    path: String? = null,
    content: CharArray? = null,
    nsContext: Iterable<Namespace> = emptyList()
) {
    configurationBuilder.imports.add(XmlResultType(name, "/$name", content, nsContext))
}

fun ConfigurableProcessModel<*>.output(
    name: String,
    refNode: Identified,
    refName: String? = null,
    path: String? = null,
    content: CharArray? = null,
    nsContext: Iterable<Namespace> = emptyList()
) {
    configurationBuilder.exports.add(XmlDefineType(name, refNode, refName, path, content, nsContext))
}
