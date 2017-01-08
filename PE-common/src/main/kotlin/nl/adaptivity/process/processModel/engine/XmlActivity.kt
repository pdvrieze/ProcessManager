/*
 * Copyright (c) 2016.
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

package nl.adaptivity.process.processModel.engine

import net.devrieze.util.collection.replaceBy
import net.devrieze.util.toMutableArraySet
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.*
import nl.adaptivity.xml.schema.annotations.XmlName


/**
 * Class representing an activity in a process engine. Activities are expected
 * to invoke one (and only one) web service. Some services are special in that
 * they either invoke another process (and the process engine can treat this
 * specially in later versions), or set interaction with the user. Services can
 * use the ActivityResponse soap header to indicate support for processes and
 * what the actual state of the task after return should be (instead of

 * @author Paul de Vrieze
 */
@XmlDeserializer(XmlActivity.Factory::class)
class XmlActivity : ActivityBase<XmlProcessNode, XmlModelCommon>, XmlProcessNode {

  constructor(builder: Activity.Builder<*, *>, newOwnerModel: XmlModelCommon) : super(builder, newOwnerModel)

  constructor(builder: Activity.ChildModelBuilder<*, *>, childModel: XmlChildModel) : super(builder, childModel)

  class Builder : ActivityBase.Builder<XmlProcessNode, XmlModelCommon>, XmlProcessNode.Builder {

    constructor() {}

    constructor(predecessor: Identified?, successor: Identified?, id: String?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>, message: XmlMessage?, condition: String?, name: String?) : super(id, predecessor, successor, label, defines, results, message, condition, name, x, y) {}

    constructor(node: Activity<*, *>) : super(node) {}

    override fun build(newOwner: XmlModelCommon): XmlActivity {
      return XmlActivity(this, newOwner)
    }
  }

  class ChildModelBuilder(
      rootBuilder: XmlProcessModel.Builder,
      override var id: String? = null,
      childId: String? = null,
      nodes: Collection<XmlProcessNode.Builder> = emptyList(),
      predecessors: Collection<Identified> = emptyList(),
      override var condition: String? = null,
      successors: Collection<Identified> = emptyList(),
      override var label: String? = null,
      imports: Collection<IXmlResultType> = emptyList(),
      defines: Collection<IXmlDefineType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList(),
      results: Collection<IXmlResultType> = emptyList(),
      override var x: Double = Double.NaN,
      override var y: Double = Double.NaN) : XmlChildModel.Builder(rootBuilder, childId, nodes, imports, exports), Activity.ChildModelBuilder<XmlProcessNode, XmlModelCommon>, XmlModelCommon.Builder {

    override var predecessors: MutableSet<Identified> = predecessors.toMutableArraySet()
      set(value) { field.replaceBy(value) }

    override var successors: MutableSet<Identified> = successors.toMutableArraySet()
      set(value) { field.replaceBy(value) }

    override var defines: MutableCollection<IXmlDefineType> = java.util.ArrayList(defines)
      set(value) {field.replaceBy(value)}

    override var results: MutableCollection<IXmlResultType> = java.util.ArrayList(results)
      set(value) {field.replaceBy(value)}

    override fun buildModel(ownerModel: RootProcessModel<XmlProcessNode, XmlModelCommon>, pedantic: Boolean): XmlChildModel {
      return XmlChildModel(this, ownerModel as XmlProcessModel, pedantic)
    }

    override fun buildActivity(childModel: ChildProcessModel<XmlProcessNode, XmlModelCommon>): Activity<XmlProcessNode, XmlModelCommon> {
      return XmlActivity(this, childModel as XmlChildModel)
    }
  }

  class Factory : XmlDeserializerFactory<XmlActivity> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): XmlActivity {
      return XmlActivity.deserialize(null, reader)
    }
  }

  private var mCondition: XmlCondition? = null

  override fun builder(): Builder {
    return Builder(this)
  }

  @Throws(XmlException::class)
  override fun serializeCondition(out: XmlWriter) {
    out.writeChild(mCondition)
  }

  /* (non-Javadoc)
         * @see nl.adaptivity.process.processModel.IActivity#getCondition()
         */
  /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.IActivity#setCondition(java.lang.String)
       */
  override var condition: String?
    @XmlName(Condition.ELEMENTLOCALNAME)
    get() = if (mCondition == null) null else mCondition!!.toString()
    set(condition) {
      mCondition = if (condition == null) null else XmlCondition(condition)
      notifyChange()
    }

  public override fun setOwnerModel(newOwnerModel: XmlModelCommon) {
    super.setOwnerModel(newOwnerModel)
  }

  public override fun setPredecessors(predecessors: Collection<Identifiable>) {
    super.setPredecessors(predecessors)
  }

  public override fun removePredecessor(predecessorId: Identified) {
    super.removePredecessor(predecessorId)
  }

  public override fun addPredecessor(predecessorId: Identified) {
    super.addPredecessor(predecessorId)
  }

  public override fun addSuccessor(successorId: Identified) {
    super.addSuccessor(successorId)
  }

  public override fun removeSuccessor(successorId: Identified) {
    super.removeSuccessor(successorId)
  }

  public override fun setSuccessors(successors: Collection<Identified>) {
    super.setSuccessors(successors)
  }

  companion object {

    @Throws(XmlException::class)
    fun deserialize(ownerModel: XmlModelCommon?, reader: XmlReader): XmlActivity {
      return XmlActivity.Builder().deserializeHelper(reader).build(ownerModel!!)
    }

    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): XmlActivity.Builder {
      return Builder().deserializeHelper(reader)
    }
  }

}

