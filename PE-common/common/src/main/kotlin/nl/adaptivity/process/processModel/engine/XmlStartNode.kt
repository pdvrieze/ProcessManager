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

package nl.adaptivity.process.processModel.engine

import kotlinx.serialization.Serializable
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.StartNodeBase
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified

@Serializable
class XmlStartNode : StartNodeBase<XmlProcessNode, XmlModelCommon>, XmlProcessNode {

    constructor(builder: StartNode.Builder<*, *>, buildHelper: BuildHelper<XmlProcessNode, XmlModelCommon>) :
        super(builder, buildHelper)

    constructor(ownerModel: XmlProcessModel) : super(ownerModel)

    constructor(ownerModel: XmlProcessModel, imports: List<XmlResultType>) : super(ownerModel) {
        setResults(imports)
    }

    override fun builder(): Builder {
        return Builder(this)
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

    @Serializable
    class Builder : StartNodeBase.Builder<XmlProcessNode, XmlModelCommon>, XmlProcessNode.Builder {

        constructor()

        constructor(base: StartNode<*, *>) : super(base)

        override fun build(buildHelper: BuildHelper<XmlProcessNode, XmlModelCommon>): XmlStartNode {
            return XmlStartNode(this, buildHelper)
        }
    }

}
