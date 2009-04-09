//
// $Id$

package com.twingle.persist

import java.nio.ByteBuffer
import java.util.Date
import java.util.UUID

/**
 * A persistent object loaded from the database which is comprised of a unique identifier and a
 * collection of attributes.
 */
trait DatabaseObject
{
  // annoying! why do I have to do this?
  import DatabaseObject.Marshaler

  /** This object's unique identifier. */
  def id :UUID = reqA(uuidM, "id").data

  /**
   * Returns the names of all attributes contained in this object.
   */
  def attrs () :Iterator[String] = _attrs.keys

  /**
   * Returns true if this object contains an attribute with the specified name.
   */
  def hasAttr (name :String) :Boolean = _attrs.contains(name)

  // from AnyRef
  override def toString = _attrs.toString

  /**
   * Returns the string value of the supplied field.
   */
  protected def value (name :String) :Option[String] = _attrs.get(name)

  /** Represents a single attribute of a database object. */
  protected abstract class Attr[T] (marsh :Marshaler[T], name :String) {
    /** Defines the type used as a container for our attribute, e.g. Option, List, etc. */
    type Container

    /** The data for this attribute, wrapped in its container. */
    def data :Container

    /** Returns the data for this attribute or the supplied default if attr is not defined. */
    def | (default: T) :T
  }

  /** Represents a required attribute. */
  protected class ReqAttr[T] (marsh :Marshaler[T], name :String) extends Attr[T](marsh, name) {
    /** Required attributes have no container as they always exist. */
    type Container = T

    /** Returns the data for this attribute. Throws an exception if it does not exist. */
    def | (default :T) :T = data

    // from Attr
    override def data :T = marsh.unmarshal(value(name).get)
  }

  /** Creates a required attribute. */
  protected def reqA[T] (marsh :Marshaler[T], name :String) = new ReqAttr[T](marsh, name)

  /** Represents an optional attribute. */
  protected class OptAttr[T] (marsh :Marshaler[T], name :String) extends Attr[T](marsh, name) {
    /** Define our container type. In this case an Option. */
    type Container = Option[T]

    /** Returns the value of this attribute or the supplied default. */
    def | (default :T) :T = data.getOrElse(default)

    // from Attr
    override def data :Option[T] = value(name) match {
      case None => None
      case Some(text) => Some(marsh.unmarshal(text))
    }
  }

  /** Creates an optional attribute. */
  protected def optA[T] (marsh :Marshaler[T], name :String) = new OptAttr[T](marsh, name)

  /** Represents a list attribute. */
  protected class ListAttr[T] (marsh :Marshaler[T], name :String) extends Attr[T](marsh, name) {
    /** List attribute data is wrapped in a List. */
    type Container = List[T]

    /** Returns the head of our list or the default if the list is empty. */
    def | (default :T) :T = if (data.isEmpty) default else data.head

    def data :List[T] = value(name) match {
      case None => Nil
      case Some(text) => text.split("\t").map(marsh.unmarshal).toList // TODO: better separator?
    }
  }

  /** Creates an attribute for an optional attribute of a database object. */
  protected def listA[T] (marsh :Marshaler[T], name :String) = new ListAttr[T](marsh, name)

  // this sucks, why aren't values and methods from DatabaseObject in scope in my trait?
  protected def uuidM = DatabaseObject.uuidM
  protected def stringM = DatabaseObject.stringM
  protected def intM = DatabaseObject.intM
  protected def dateM = DatabaseObject.dateM
  protected def byteBufferM = DatabaseObject.byteBufferM

  // I don't like this, need to figure out something better
  private[persist] def init (attrs :Map[String, String]) {
    _attrs = attrs
  }

  private[this] var _attrs :Map[String, String] = null
}

object DatabaseObject
{
  class Builder {
    protected def add[T <: AnyRef] (name :String, value :T) {
      _map += (name -> marshalers.get(value.getClass).get.asInstanceOf[Marshaler[T]].marshal(value))
    }

    protected def add[T <: AnyRef] (name :String, value :Option[T]) {
      value match {
        case None => //
          case Some(v) => add(name, v)
      }
    }

    protected def build[T <: DatabaseObject] (obj :T) = {
      _map += ("id" -> uuidM.marshal(UUID.randomUUID))
      obj.init(_map)
      obj
    }

    private[this] var _map = Map[String, String]()
  }

  def builder = new Builder

  /** Used to convert strings to and from target data types. */
  protected trait Marshaler[T] {
    /** Marshals a value for this attribute into a string representation. */
    def marshal (value :T) :String

    /** Unmarshals a value for this attribute from its underlying string representation. */
    def unmarshal (value :String) :T
  }

  /** Creates a marshaler for {@link UUID} fields. */
  protected val uuidM :Marshaler[UUID] = new Marshaler[UUID] {
    def marshal (value :UUID) = value.toString
    def unmarshal (value :String) = UUID.fromString(value)
  }

  /** Creates a marshaler for {@link String} fields. */
  protected val stringM :Marshaler[String] = new Marshaler[String] {
    def marshal (value :String) = value
    def unmarshal (value :String) = value
  }

  /** Creates a marshaler for Int fields. */
  protected val intM :Marshaler[Int] = new Marshaler[Int] {
    def marshal (value :Int) = value.toString
    def unmarshal (value :String) = java.lang.Integer.parseInt(value)
  }

  /** Creates a marshaler for {@link Date} fields. */
  protected val dateM :Marshaler[Date] = new Marshaler[Date] {
    def marshal (value :Date) = value.getTime.toString
    def unmarshal (value :String) = new Date(java.lang.Long.parseLong(value))
  }

  /** Creates a marshaler for {@link ByteBuffer} fields. */
  protected val byteBufferM :Marshaler[ByteBuffer] = new Marshaler[ByteBuffer] {
    def marshal (value :ByteBuffer) = new String(value.array, "ISO-8859-1")
    def unmarshal (value :String) = ByteBuffer.wrap(value.getBytes("ISO-8859-1"))
  }

  protected val marshalers = scala.collection.mutable.Map[Class[_], Marshaler[_]]()
  marshalers += (classOf[UUID] -> uuidM)
  marshalers += (classOf[String] -> stringM)
  marshalers += (classOf[Int] -> intM)
  marshalers += (classOf[Date] -> dateM)
  marshalers += (classOf[ByteBuffer] -> byteBufferM)
}
