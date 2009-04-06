//
// $Id$

package com.twingle.persist

/**
 * Represents an optional attribute of a database object.
 */
trait OptAttr[T] extends Attribute[T]
{
  /** Define our container type. In this case an Option. */
  type Container = Option[T]

  /** Returns the value of this attribute or the supplied default. */
  def | (default :T) :T = data.getOrElse(default)
}
