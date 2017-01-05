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

package nl.adaptivity.process.processModel

import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.smartStartTag
import nl.adaptivity.xml.writeChildren
import javax.xml.namespace.QName

/**
 * Base class for submodels
 */
abstract class ChildProcessModelBase<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : ProcessModelBase<T, M>, ChildProcessModel<T,M> {

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?>(override var childId: String? = null,
                                                                         nodes: Collection<ProcessNode.Builder<T, M>> = emptyList(),
                                                                         imports: Collection<IXmlResultType> = emptyList(), exports: Collection<IXmlDefineType> = emptyList()) : ProcessModelBase.Builder<T,M>(nodes, imports, exports), ChildProcessModel.Builder<T,M> {
    override abstract fun buildModel(ownerModel: M, pedantic: Boolean): ChildProcessModelBase<T, M>
  }

  constructor(builder: ChildProcessModel.Builder<T,M>, ownerModel: M, pedantic: Boolean):super(builder, pedantic) {
    this.ownerModel = ownerModel
    this.childId = builder.childId?.let(::Identifier)
  }

  override val ownerModel: M

  override val childId: Identifiable?

  abstract override fun builder(): ChildProcessModelBase.Builder<T, M>

  override fun serialize(out: XmlWriter) {
    if (getModelNodes().any { it.id == null }) {
      builder().buildModel(ownerModel).serialize(out)
    } else {
      out.smartStartTag(ELEMENTNAME) {
        writeChildren(imports)
        writeChildren(exports)
        writeChildren(getModelNodes())
      }
    }
  }

  companion object {
    @JvmField
    val ELEMENTLOCALNAME = "childModel"
    val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME, ProcessConsts.Engine.NSPREFIX)
  }

}