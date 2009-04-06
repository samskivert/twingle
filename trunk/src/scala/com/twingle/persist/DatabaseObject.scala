//
// $Id$

package com.twingle.persist

import java.util.UUID

/**
 * A persistent object loaded from the database which is comprised of a unique identifier and a
 * collection of attributes.
 */
trait DatabaseObject
{
  /** This object's unique identifier. */
  def id :UUID = attr[UUID, ReqAttr[UUID]](classOf[ReqAttr[UUID]], "id").data

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
  def attr[A, B <: Attribute[A]] (aclass :Class[B], name :String) :B = throw new Error()
}
