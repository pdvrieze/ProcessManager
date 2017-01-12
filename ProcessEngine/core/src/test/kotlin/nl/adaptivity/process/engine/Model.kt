/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.engine

import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import java.security.Principal
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Created by pdvrieze on 12/01/17.
 */

internal typealias Model = ConfigurableModel

internal abstract class ConfigurableModel(
    override val owner: Principal,
    private val name: String? = null,
    private val uuid: UUID = UUID.randomUUID()
  ) : RootProcessModel<ExecutableProcessNode, ExecutableModelCommon> {

  class NodeDelegate<T: Identifiable>(override val id:String): ReadOnlyProperty<ConfigurableModel, T>, Identifiable {
    override fun getValue(thisRef: ConfigurableModel, property: KProperty<*>): T {
      @Suppress("UNCHECKED_CAST")
      return when {
        thisRef.builder!=null -> this
        else                  -> thisRef.model.getNode(id)
      } as T // Really nasty hack to allow node references to be used at definition time
    }
  }

  class ChildDelegate<T: Identifiable>(override val id:String): ReadOnlyProperty<ConfigurableModel, T>, Identifiable {
    override fun getValue(thisRef: ConfigurableModel, property: KProperty<*>): T {
      @Suppress("UNCHECKED_CAST")
      return when {
        thisRef.builder!=null -> this
        else                  -> thisRef.model.getChildModel(this)
      } as T // Really nasty hack to allow node references to be used at definition time
    }
  }

  inner class NodeBinder {
    inline operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = nodeRef(property.name)
  }

  inner class ChildBinder {
    inline operator fun provideDelegate(thisRef: Any?, property: KProperty<*>)= childRef(property.name)
  }

  private var builder: ExecutableProcessModel.Builder? = null

  private val model: ExecutableProcessModel by lazy { builder!!.build(false).apply { builder=null } }

  operator fun ExecutableProcessNode.Builder.provideDelegate(thisRef:ConfigurableModel, property: KProperty<*>): Identifier {
    val modelBuilder = builder?:let { ExecutableProcessModel.Builder().apply{builder = this} }
    val nodeBuilder = this
    if (id==null && modelBuilder.nodes.firstOrNull { it.id == property.name }==null) id = property.name
    with(modelBuilder) {
      if (nodeBuilder is ExecutableActivity.ChildModelBuilder) {
        childModels.add(nodeBuilder.ensureChildId())
      }

      nodes.add(nodeBuilder.ensureId())
    }
    return Identifier(id!!)
  }

  protected operator inline fun Identifier.getValue(thisRef: ConfigurableModel, property: KProperty<*>): Identifier = this

  override fun getName() = name
  override fun getUuid() = uuid

  @Suppress("NOTHING_TO_INLINE")
  inline fun childRef(childId:String) = lazy { model.getChildModel(Identifier(childId)) }
  inline val childRef get() = ChildBinder()
  @Suppress("NOTHING_TO_INLINE")
  inline fun nodeRef(nodeId:String) = lazy { (model.getModelNodes().asSequence() + model.childModels.asSequence().flatMap { it.getModelNodes().asSequence() }).firstOrNull { it.id==nodeId } }
  inline val nodeRef get() = NodeBinder()
  inline protected val startNode get() = ExecutableStartNode.Builder()
  inline protected fun startNode(config: ExecutableStartNode.Builder.()->Unit) = ExecutableStartNode.Builder().apply(config)
  @Suppress("NOTHING_TO_INLINE")
  inline protected fun activity(predecessor: Identified) = ExecutableActivity.Builder(predecessor = predecessor)
  inline protected fun activity(predecessor: Identified, config: ExecutableActivity.Builder.()->Unit) = ExecutableActivity.Builder(predecessor = predecessor).apply(config)
  @Suppress("NOTHING_TO_INLINE")
  inline protected fun compositeActivity(predecessor: Identified) = ExecutableActivity.ChildModelBuilder(builder!!, predecessor = predecessor)
  inline protected fun compositeActivity(predecessor: Identified, config: ExecutableActivity.ChildModelBuilder.()->Unit) = ExecutableActivity.ChildModelBuilder(builder!!, predecessor = predecessor).apply(config)
  @Suppress("NOTHING_TO_INLINE")
  inline protected fun split(predecessor: Identified) = ExecutableSplit.Builder(predecessor = predecessor)
  inline protected fun split(predecessor: Identified, config: ExecutableSplit.Builder.()->Unit) = ExecutableSplit.Builder(predecessor = predecessor).apply(config)
  @Suppress("NOTHING_TO_INLINE")
  inline protected fun join(vararg predecessors: Identified) = ExecutableJoin.Builder(predecessors = Arrays.asList(*predecessors))
  inline protected fun join(predecessors: Collection<Identified>) = ExecutableJoin.Builder(predecessors = predecessors)
  inline protected fun join(vararg predecessors: Identified, config: ExecutableJoin.Builder.()->Unit) = ExecutableJoin.Builder(predecessors = Arrays.asList(*predecessors)).apply(config)
  inline protected fun join(predecessors: Collection<Identified>, config: ExecutableJoin.Builder.()->Unit) = ExecutableJoin.Builder(predecessors = predecessors).apply(config)
  @Suppress("NOTHING_TO_INLINE")
  inline protected fun endNode(predecessor: Identified) = ExecutableEndNode.Builder(predecessor = predecessor)
  inline protected fun endNode(predecessor: Identified, config: ExecutableEndNode.Builder.()->Unit) = ExecutableEndNode.Builder(predecessor = predecessor).apply(config)

  override fun getRef() = model.getRef()

  override fun getNode(nodeId: Identifiable) = model.getNode(nodeId)

  override fun getModelNodes() = model.getModelNodes()

  override val childModels get() = model.childModels

  override fun getChildModel(childId: Identifiable) = model.getChildModel(childId)

  override fun getRoles() = model.getRoles()

  override fun getImports() = model.getImports()

  override fun getExports() = model.getExports()
}