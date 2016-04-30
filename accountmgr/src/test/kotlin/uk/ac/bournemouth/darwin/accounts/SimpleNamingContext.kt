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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package uk.ac.bournemouth.darwin.accounts

import java.util.*
import javax.naming.*

/**
 * Created by pdvrieze on 29/04/16.
 */

class SimpleNamingEnumeration<T>(sequence: Sequence<T>) :NamingEnumeration<T> {
  val iterator = sequence.iterator()

  override fun next() = nextElement()

  override fun hasMore() = hasMoreElements()

  override fun close() { /* do nothing */ }

  override fun nextElement() = iterator.next()

  override fun hasMoreElements() = iterator.hasNext()

}

fun Name.asSequence():Sequence<String> {
  return all.asSequence()
}
class SimpleName(components:Collection<String>): Name {
  val components: MutableList<String> = ArrayList(components)


  constructor(vararg components:String): this(components.toList())

  override fun addAll(suffix: Name): Name {
    components.addAll(suffix.asSequence())
    return this
  }

  override fun addAll(posn: Int, n: Name): Name {
    components.addAll(posn, n.asSequence().toList())
    return this
  }

  override fun isEmpty() = components.size==0

  override fun compareTo(other: Any): Int {
    if (other is CharSequence) {
      return if (isEmpty()) -1 else get(0).compareTo(other.toString())
    }
    val otherName = other as Name
    components.forEachIndexed { i, s ->
      if (i>=otherName.size()) return 1
      val cmp = s.compareTo(otherName.get(i))
      if (cmp!=0) { return cmp }
    }
    return if (size()<other.size()) -1 else 0; // If they are not different in length then they must be equal
  }

  override fun getAll(): Enumeration<String>? {
    return SimpleNamingEnumeration(components.asSequence())
  }

  override fun remove(posn: Int): String {
    val item = components.removeAt(posn)
    return item
  }

  override fun startsWith(n: Name): Boolean {
    if (size()<n.size()) { return false; }
    components.forEachIndexed { i, s -> if (n.get(i)!=s) return false }
    return true;
  }

  override fun endsWith(n: Name): Boolean {
    if (size()<n.size()) { return false; }
    val offset = size()-n.size()
    components.indices.reversed().forEach{ i -> if (n.get(i-offset)!=get(i)) return false }
    return true;
  }

  override fun add(comp: String): Name {
    components.add(comp)
    return this
  }

  override fun add(posn: Int, comp: String): Name {
    components.add(posn, comp)
    return this
  }

  override fun clone(): SimpleName {
    return SimpleName(ArrayList(components))
  }

  override fun getPrefix(posn: Int): Name {
    return SimpleName(components.subList(0, posn))
  }

  override fun getSuffix(posn: Int): Name {
    return SimpleName(components.subList(posn, components.size))
  }

  override fun size(): Int {
    return components.size
  }

  override fun get(posn: Int): String {
    return components[posn]
  }

  override fun toString(): String{
    return "SimpleName(components=$components)"
  }


}


class SimpleNameParser: NameParser {
  override fun parse(name: String): Name {
    val result= mutableListOf<String>()
    fun doParse(name:String) {
      val colPos = name.indexOf(':')
      val slashPos = name.indexOf('/')
      if (colPos<0 && slashPos<0) {
        result.add(name)
      } else if (slashPos>=0 && (slashPos<colPos || colPos<0)) {
        if (slashPos>0) result.add(name.substring(0, slashPos))
        doParse(name.substring(slashPos+1))
      } else if (colPos>=0 && (colPos<slashPos || slashPos<0)) {
        // ignore namespaces
//        if (colPos>0) result.add(name.substring(0, colPos))
        doParse(name.substring(colPos+1))
      }
    }

    doParse(name)

    return SimpleName(result)
  }
}

class SimpleNamingContext:Context {
  companion object {
    private val parser = SimpleNameParser()
  }

  private val _bindings = mutableListOf<Binding>()
  val bindings:List<Binding>
    get() = _bindings

  override fun listBindings(name: Name): NamingEnumeration<Binding> {
    val seq: Sequence<Binding> = bindings.asSequence()
          .filter { it.name == name.get(0) }
          .flatMap { binding ->
            if (name.size() == 1) {
              sequenceOf(binding)
            } else {
              val obj = binding.`object`
              if (obj is Context) {
                obj.listBindings(name.getSuffix(1)).asSequence()
              } else throw NamingException("Name extends, child is not a context")
            }
          }
    return SimpleNamingEnumeration<Binding>(seq)
  }

  override fun listBindings(name: String): NamingEnumeration<Binding> {
    return listBindings(parseName(name))
  }

  private fun parseName(name: String) = parser.parse(name)

  private fun getParent(name:Name):Context {
    return lookup(name.getPrefix(name.size()-1)) as? Context ?: throw NotContextException("The name $name does not resolve to a context")
  }

  override fun destroySubcontext(name: Name) {
    val elem = let {
      val pos = bindings.indexOfFirst { it.name==name.get(0) }
      if (pos<0) { throw NameNotFoundException("The request") }
      bindings.get(pos)
    }
    throw UnsupportedOperationException()
  }

  override fun destroySubcontext(name: String) {
    destroySubcontext(parseName(name))
  }

  override fun list(name: Name?): NamingEnumeration<NameClassPair>? {
    throw UnsupportedOperationException()
  }

  override fun list(name: String?): NamingEnumeration<NameClassPair>? {
    throw UnsupportedOperationException()
  }

  override fun composeName(name: Name?, prefix: Name?): Name? {
    throw UnsupportedOperationException()
  }

  override fun composeName(name: String?, prefix: String?): String? {
    throw UnsupportedOperationException()
  }

  override fun createSubcontext(name: Name): Context {
    val result = SimpleNamingContext()
    bind(name, result)
    return result
  }

  override fun createSubcontext(name: String): Context {
    return createSubcontext(parseName(name))
  }

  override fun rename(oldName: Name?, newName: Name?) {
    throw UnsupportedOperationException()
  }

  override fun rename(oldName: String?, newName: String?) {
    throw UnsupportedOperationException()
  }

  override fun addToEnvironment(propName: String?, propVal: Any?): Any? {
    throw UnsupportedOperationException()
  }

  override fun getNameParser(name: Name?): NameParser {
    return parser
  }

  override fun getNameParser(name: String?): NameParser {
    return parser
  }

  override fun close() {
    _bindings.clear()
  }

  override fun lookup(name: Name): Any {
    return name.asSequence().fold(this) { obj:Any, component ->
      if (obj !is Context) throw NotContextException("An intermediate object for ${name} object for name is not a context")
      if (obj is SimpleNamingContext) {
        val resolvedObject = obj._bindings.firstOrNull { it.name == component }?.`object`
        resolvedObject ?: throw NameNotFoundException("No element with name $component (from $name) could be found in the context")
      } else {
        obj.lookup(component)
      } as Any
    }
  }

  override fun lookup(name: String) = lookup(parseName(name))

  override fun getNameInNamespace(): String? {
    throw UnsupportedOperationException()
  }

  override fun rebind(name: Name, obj: Any?) {
    throw UnsupportedOperationException()
  }

  override fun rebind(name: String, obj: Any?) {
    throw UnsupportedOperationException()
  }

  override fun unbind(name: Name) {
    throw UnsupportedOperationException()
  }

  override fun unbind(name: String?) {
    throw UnsupportedOperationException()
  }

  override fun getEnvironment(): Hashtable<*, *>? {
    throw UnsupportedOperationException()
  }

  override fun bind(name: Name, obj: Any?) {
    if (name.size()==1) {
      _bindings.add(Binding(name.get(0), obj))
    } else {
      val parent = getParent(name)
      return parent.bind(name.getSuffix(name.size() - 1), obj)
    }
  }

  override fun bind(name: String, obj: Any?) {
    bind(parseName(name), obj)
  }

  override fun removeFromEnvironment(propName: String?): Any? {
    throw UnsupportedOperationException()
  }

  override fun lookupLink(name: Name): Any? {
    throw UnsupportedOperationException()
  }

  override fun lookupLink(name: String): Any? {
    throw UnsupportedOperationException()
  }
}