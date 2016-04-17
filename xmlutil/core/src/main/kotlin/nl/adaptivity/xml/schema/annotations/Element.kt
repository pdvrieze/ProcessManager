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

package nl.adaptivity.xml.schema.annotations

import nl.adaptivity.xml.XmlSerializable
import kotlin.reflect.KClass

/**
 * Created by pdvrieze on 14/04/16.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Element(val name:String, val nsUri:String, val nsPrefix:String="", val attributes:Array<Attribute> = arrayOf(), val children:Array<Child>)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Child(val name:String="", val property:String="", val type:KClass<out Any>)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Attribute(val value:String, val default: String="", val optional:Boolean=true)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FIELD)
annotation class XmlName(val value:String, val nsUri:String = "")

class AnyType() // Class with the only purpose to annotate an any type
