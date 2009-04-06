//
// $Id$

package com.twingle.persist

/**
 * Represents a list of attributes in a database object.
 */
trait ListAttr[T] extends Attribute[T]
{
  /** List attribute data is wrapped in a List. */
  type Container = List[T]

  /** Returns the head of our list or the default if the list is empty. */
  def | (default :T) :T = if (data.isEmpty) default else data.head
}
