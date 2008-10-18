//
// $Id$

package com.twingle.persist

/**
 * A persistent object loaded from the database which is comprised of a unique identifier and a
 * collection of attributes.
 */
class DatabaseObject
{
  /** This object's unique identifier. */
  val id :Long = 0L

  /**
   * Returns a list of all attributes contained by this object.
   */
  def attrs () :List[Attribute[Any]] = throw new Error()

  /**
   * Returns true if this object contains an attribute with the specified name.
   */
  def hasAttr (name :String) :Boolean = throw new Error()

  /**
   * Returns the specified attribute or a blank attribute if this object does
   * not contain the attribute in question. TODO: if it contains an attribute
   * of the same name but the wrong type, do we return a blank attribute or fail?
   */
  def attr[A >: Attribute[Any]] (aclass :Class[A], name :String) :A = throw new Error()
}
