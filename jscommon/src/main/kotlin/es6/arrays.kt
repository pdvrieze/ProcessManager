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

@file:Suppress("NOTHING_TO_INLINE")

package es6

@JsName("Array")
external interface JsArray<T> {
  /**
   * Appends new elements to an array, and returns the new length of the array.
   * @param values New elements of the Array.
   */
  fun push(vararg values:T): Int

  /**
   * Removes the last element from an array and returns it.
   */
  fun pop(): T

  /**
   * Removes the first element from an array and returns it.
   */
  fun shift(): T

  /**
   * Inserts new elements at the start of an array.
   * @param values  Elements to insert at the start of the Array.
   */
  fun unshift(vararg values: T): Int

  /**
   * Returns a section of an array.
   * @param start The beginning of the specified portion of the array.
   * @param end The end of the specified portion of the array.
   */
  fun slice(begin:Int = definedExternally, end: Int = definedExternally): Array<T>

  operator fun get(index: Int): T

  /**
   * Gets or sets the length of the array. This is a number one higher than the highest element defined in an array.
   */
  var length: Int

  /**
   * Combines two or more arrays.
   * @param items Additional items to add to the end of array1.
   */
  fun concat(vararg items: JsArray<T>): JsArray<T>

  /**
   * Combines two or more arrays.
   * @param items Additional items to add to the end of array1.
   */
  fun concat(vararg items: T): JsArray<T>

  /**
   * Adds all the elements of an array separated by the specified separator string.
   * @param separator A string used to separate one element of an array from the next in the resulting String. If omitted, the array elements are separated with a comma.
   */
  fun join(separator: String? = definedExternally): String

  /**
   * Reverses the elements in an Array.
   */
  fun reverse(): JsArray<T>

  /**
   * Sorts an array.
   * @param compareFn The name of the function used to determine the order of the elements. If omitted, the elements are sorted in ascending, ASCII character order.
   */
  fun sort(compareFn: (a: T, b: T) -> Int = definedExternally): JsArray<T>

  /**
   * Removes elements from an array and, if necessary, inserts new elements in their place, returning the deleted elements.
   * @param start The zero-based location in the array from which to start removing elements.
   */
  fun splice(start: Int): JsArray<T>

  /**
   * Removes elements from an array and, if necessary, inserts new elements in their place, returning the deleted elements.
   * @param start The zero-based location in the array from which to start removing elements.
   * @param deleteCount The number of elements to remove.
   * @param items Elements to insert into the array in place of the deleted elements.
   */
  fun splice(start: Int, deleteCount: Int, vararg items: T = definedExternally): JsArray<T>

  /**
   * Returns the index of the first occurrence of a value in an array.
   * @param searchElement The value to locate in the array.
   * @param fromIndex The array index at which to begin the search. If fromIndex is omitted, the search starts at index 0.
   */
  fun indexOf(searchElement: T, fromIndex: Int = definedExternally): Int

  /**
   * Returns the index of the last occurrence of a specified value in an array.
   * @param searchElement The value to locate in the array.
   * @param fromIndex The array index at which to begin the search. If fromIndex is omitted, the search starts at the last index in the array.
   */
  fun lastIndexOf(searchElement: T, fromIndex: Int = definedExternally): Int

  /**
   * Determines whether all the members of an array satisfy the specified test.
   * @param callbackfn A function that accepts up to three arguments. The every method calls the callbackfn function for each element in array1 until the callbackfn returns false, or until the end of the array.
   * @param thisArg An object to which the this keyword can refer in the callbackfn function. If thisArg is omitted, undefined is used as the this value.
   */
  fun every(callbackfn: (value: T, index: Int, array: JsArray<T>) -> Boolean,
            thisArg: Any? = definedExternally): Boolean

  /**
   * Determines whether the specified callback function returns true for any element of an array.
   * @param callbackfn A function that accepts up to three arguments. The some method calls the callbackfn function for each element in array1 until the callbackfn returns true, or until the end of the array.
   * @param thisArg An object to which the this keyword can refer in the callbackfn function. If thisArg is omitted, undefined is used as the this value.
   */
  fun some(callbackfn: (value: T, index: Int, array: JsArray<T>) -> Boolean,
           thisArg: Any? = definedExternally): Boolean

  /**
   * Performs the specified action for each element in an array.
   * @param callbackfn  A function that accepts up to three arguments. forEach calls the callbackfn function one time for each element in the array.
   * @param thisArg  An object to which the this keyword can refer in the callbackfn function. If thisArg is omitted, undefined is used as the this value.
   */
  fun forEach(callbackfn: (value: T, index: Int, array: JsArray<T>) -> Unit, thisArg: Any? = definedExternally)

  /**
   * Calls a defined callback function on each element of an array, and returns an array that contains the results.
   * @param callbackfn A function that accepts up to three arguments. The map method calls the callbackfn function one time for each element in the array.
   * @param thisArg An object to which the this keyword can refer in the callbackfn function. If thisArg is omitted, undefined is used as the this value.
   */
  fun <U> map(callbackfn: (value: T, index: Int, array: JsArray<T>) -> U,
              thisArg: Any? = definedExternally): JsArray<U>

  /**
   * Returns the elements of an array that meet the condition specified in a callback function.
   * @param callbackfn A function that accepts up to three arguments. The filter method calls the callbackfn function one time for each element in the array.
   * @param thisArg An object to which the this keyword can refer in the callbackfn function. If thisArg is omitted, undefined is used as the this value.
   */
  fun filter(callbackfn: (value: T, index: Int, array: JsArray<T>) -> Any,
             thisArg: Any? = definedExternally): JsArray<T>

  /**
   * Calls the specified callback function for all the elements in an array. The return value of the callback function is the accumulated result, and is provided as an argument in the next call to the callback function.
   * @param callbackfn A function that accepts up to four arguments. The reduce method calls the callbackfn function one time for each element in the array.
   * @param initialValue If initialValue is specified, it is used as the initial value to start the accumulation. The first call to the callbackfn function provides this value as an argument instead of an array value.
   */
  fun reduce(callbackfn: (previousValue: T, currentValue: T, currentIndex: Int, array: JsArray<T>) -> T,
             initialValue: T? = definedExternally): T

  /**
   * Calls the specified callback function for all the elements in an array. The return value of the callback function is the accumulated result, and is provided as an argument in the next call to the callback function.
   * @param callbackfn A function that accepts up to four arguments. The reduce method calls the callbackfn function one time for each element in the array.
   * @param initialValue If initialValue is specified, it is used as the initial value to start the accumulation. The first call to the callbackfn function provides this value as an argument instead of an array value.
   */
  fun <U> reduce(callbackfn: (previousValue: U, currentValue: T, currentIndex: Int, array: JsArray<T>) -> U,
                 initialValue: U): U

  /**
   * Calls the specified callback function for all the elements in an array, in descending order. The return value of the callback function is the accumulated result, and is provided as an argument in the next call to the callback function.
   * @param callbackfn A function that accepts up to four arguments. The reduceRight method calls the callbackfn function one time for each element in the array.
   * @param initialValue If initialValue is specified, it is used as the initial value to start the accumulation. The first call to the callbackfn function provides this value as an argument instead of an array value.
   */
  fun reduceRight(callbackfn: (previousValue: T, currentValue: T, currentIndex: Int, array: JsArray<T>) -> T,
                  initialValue: T? = definedExternally): T

  /**
   * Calls the specified callback function for all the elements in an array, in descending order. The return value of the callback function is the accumulated result, and is provided as an argument in the next call to the callback function.
   * @param callbackfn A function that accepts up to four arguments. The reduceRight method calls the callbackfn function one time for each element in the array.
   * @param initialValue If initialValue is specified, it is used as the initial value to start the accumulation. The first call to the callbackfn function provides this value as an argument instead of an array value.
   */
  fun <U> reduceRight(callbackfn: (previousValue: U, currentValue: T, currentIndex: Int, array: JsArray<T>) -> U,
                      initialValue: U): U
}

@Suppress("UNCHECKED_CAST", "UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
inline val <T> Array<T>.jsext: JsArray<T> get() = this as JsArray<T>

inline fun <T> Array<T>.push(vararg values: T) = jsext.push(*values)
inline fun <T> Array<T>.pop() = jsext.pop()
inline fun <T> Array<T>.shift() = jsext.shift()
inline fun <T> Array<T>.unshift(vararg values: T) = jsext.unshift(*values)
inline fun <T> Array<T>.reverse() = jsext.reverse().unsafeCast<Array<T>>()
inline fun <T> Array<T>.indexOf(searchElement: T) = jsext.indexOf(searchElement)
inline fun <T> Array<T>.indexOf(searchElement: T, fromIndex: Int) = jsext.indexOf(searchElement, fromIndex)
inline fun <T> Array<T>.lastIndexOf(searchElement: T) = jsext.lastIndexOf(searchElement)
inline fun <T> Array<T>.lastIndexOf(searchElement: T, fromIndex: Int) = jsext.lastIndexOf(searchElement, fromIndex)

operator fun <T> JsArray<T>.iterator(): Iterator<T> {
    return JsArrayIterator(this)
}

inline fun <T> JsArray<T>.asSequence() = iterator().asSequence()

private class JsArrayIterator<T>(private val array: JsArray<T>): Iterator<T> {
    private var nextPos = 0

    override fun hasNext(): Boolean = nextPos <array.length

    override fun next(): T = array[nextPos++]
}
