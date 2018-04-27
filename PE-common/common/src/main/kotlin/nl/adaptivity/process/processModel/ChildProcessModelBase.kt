/*
 * Copyright (c) 2018.
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

package nl.adaptivity.process.processModel

import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.util.multiplatform.JvmField
import nl.adaptivity.xml.*

/**
 * Base class for submodels
 */
abstract class ChildProcessModelBase<T : ProcessNode<T, M>,
  M : ProcessModel<T, M>?>(builder: ChildProcessModel.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<T, M>) :
  ProcessModelBase<T, M>(builder, buildHelper.pedantic), ChildProcessModel<T,M> {

  override val _processNodes: IdentifyableSet<T> = buildNodes(builder, buildHelper.withOwner(asM))

  override val rootModel: RootProcessModel<T, M> = buildHelper.newOwner?.rootModel ?: throw IllegalProcessModelException("Childmodels must have roots")

  override val id: String? = builder.childId

  override abstract fun builder(rootBuilder: RootProcessModel.Builder<T, M>): ChildProcessModelBase.Builder<T, M>

  override fun serialize(out: XmlWriter) {
    out.smartStartTag(ELEMENTNAME) {
      writeAttribute(ATTR_ID, id)

      writeChildren(imports)
      writeChildren(exports)
      writeChildren(getModelNodes())
    }
  }

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?>(
    override val rootBuilder: RootProcessModel.Builder<T, M>,
    override var childId: String? = null,
    nodes: Collection<ProcessNode.IBuilder<T, M>> = emptyList(),
    imports: Collection<IXmlResultType> = emptyList(),
    exports: Collection<IXmlDefineType> = emptyList()) : ProcessModelBase.Builder<T,M>(nodes, imports, exports), ChildProcessModel.Builder<T,M> {

    override val elementName: QName get() = ELEMENTNAME

    /**
     * When this is overridden and it returns a non-`null` value, it will allow childmodels to be nested in eachother.
     * Note that this does not actually introduce a scope. The nesting is not retained.
     */
    open fun nestedBuilder(): ChildProcessModelBase.Builder<T,M>? = null

    override abstract fun buildModel(buildHelper: ProcessModel.BuildHelper<T, M>): ChildProcessModel<T, M>

    override fun deserializeChild(reader: XmlReader): Boolean {
      if (reader.isElement(ProcessConsts.Engine.NAMESPACE, ChildProcessModel.ELEMENTLOCALNAME)) {
        nestedBuilder()?.let { rootBuilder.childModels.add(deserializeHelper(reader)) } ?:
        reader.unhandledEvent("Child models are not currently allowed to be nested")
        return true
      } else {
        return super.deserializeChild(reader)
      }
    }

    override fun deserializeAttribute(attributeNamespace: CharSequence,
                                      attributeLocalName: CharSequence,
                                      attributeValue: CharSequence): Boolean {
      return when(attributeLocalName) {
        ATTR_ID -> { childId = attributeValue.toString(); true }
        else -> super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
      }
    }
  }

  companion object {
    const val ATTR_ID = "id"
    const val ELEMENTLOCALNAME = "childModel"
    @JvmField
    val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME,
                                              ProcessConsts.Engine.NSPREFIX)
  }

}