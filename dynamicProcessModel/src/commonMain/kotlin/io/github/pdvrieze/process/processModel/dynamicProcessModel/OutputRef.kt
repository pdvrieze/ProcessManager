package io.github.pdvrieze.process.processModel.dynamicProcessModel

import nl.adaptivity.process.util.Identified

interface OutputRef<T>: ProcessResultRef<T> {
    override val nodeRef: Identified
}
