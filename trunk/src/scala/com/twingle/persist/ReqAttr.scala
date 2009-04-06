//
// $Id$

package com.twingle.persist

/**
 * Represents a required attribute of a database object.
 */
trait ReqAttr[T] extends Attribute[T]
{
  /** Required attributes have no container as the always exist. */
  type Container = T

  /** Returns the data for this attribute (which will always exist as this attribute is required). */
  def | (default :T) :T = data
}
