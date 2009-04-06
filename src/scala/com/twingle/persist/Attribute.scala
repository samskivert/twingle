//
// $Id$

package com.twingle.persist

/**
 * Represents a single attribute of a database object.
 */
abstract trait Attribute[T]
{
  /** Defines the type used as a container for our attribute, e.g. Option, List, etc. */
  type Container

  /** The data for this attribute, wrapped in its container. */
  def data :Container

  /** Returns the data for this attribute or the supplied default if the attribute is not defined. */
  def | (default: T) :T
}
