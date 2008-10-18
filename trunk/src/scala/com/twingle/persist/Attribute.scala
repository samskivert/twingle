//
// $Id$

package com.twingle.persist

/**
 * Represents a single attribute of a database object.
 */
abstract class Attribute[T]
{
  val values :List[T] = Nil

  def getOrElse (defval :T) :T = values.firstOption.getOrElse(defval)
}
