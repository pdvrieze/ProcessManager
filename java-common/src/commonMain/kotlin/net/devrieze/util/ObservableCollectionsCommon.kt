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

package net.devrieze.util

expect abstract class ObservableCollectionBase<C : MutableCollection<T>, T, S : ObservableCollectionBase<C, T, S>>

    : Collection<T>, MutableCollection<T> {

}

expect class ObservableCollection<T>
    constructor(delegate: MutableCollection<T>, observers: Iterable<(ObservableCollection<T>)->Unit> = emptyList())
    : ObservableCollectionBase<MutableCollection<T>, T, ObservableCollection<T>>

expect class ObservableSet<T>
    constructor(delegate: MutableSet<T>, observers: Iterable<(ObservableSet<T>)->Unit> = emptyList())
    : ObservableCollectionBase<MutableSet<T>, T, ObservableSet<T>>, MutableSet<T>

expect class ObservableList<T>
    constructor(delegate: MutableList<T>, observers: Iterable<(ObservableList<T>) -> Unit> = emptyList()) :
    ObservableCollectionBase<MutableList<T>, T, ObservableList<T>>, List<T>, MutableList<T>
