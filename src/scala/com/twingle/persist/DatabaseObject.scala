//
// $Id$

package com.twingle.persist

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.nio.ByteBuffer
import java.util.Date
import java.util.Properties
import java.util.UUID

/**
 * A persistent object loaded from the database which is comprised of a unique identifier and a
 * collection of attributes.
 */
trait DatabaseObject
{
  import DatabaseObject.Marshaler

  /** This object's unique identifier. */
  def id :UUID = reqA(uuidM, 'id).data

  /** Returns the names of all attributes contained in this object. */
  def attrs () :Iterator[Symbol] = _attrs.keys

  /** Returns true if this object contains an attribute with the specified name. */
  def hasAttr (name :Symbol) :Boolean = _attrs.contains(name)

  /** Returns a brief string identifier for this instance. */
  def idString = getClass.getSimpleName + ":" + id

  // from AnyRef
  override def toString = _attrs.toString

  /**
   * Returns the string value of the supplied attr.
   */
  protected def value (name :Symbol) :Option[String] = _attrs.get(name)

  /** Represents a single attribute of a database object. */
  protected abstract class Attr[T] (marsh :Marshaler[T], name :Symbol) {
    /** Defines the type used as a container for our attribute, e.g. Option, List, etc. */
    type Container

    /** The data for this attribute, wrapped in its container. */
    def data :Container

    /** Returns the marshaller used by this attribute. */
    private[persist] def marshaller = marsh
  }

  /** Represents a required attribute. */
  protected class ReqAttr[T] (marsh :Marshaler[T], name :Symbol) extends Attr[T](marsh, name) {
    /** Required attributes have no container as they always exist. */
    type Container = T

    // from Attr
    override def data :T = marsh.unmarshal(value(name).get)
  }

  /** Creates a required attribute. */
  protected def reqA[T] (marsh :Marshaler[T], name :Symbol) = new ReqAttr[T](marsh, name)

  /** Represents an optional attribute. */
  protected class OptAttr[T] (marsh :Marshaler[T], name :Symbol) extends Attr[T](marsh, name) {
    /** Define our container type. In this case an Option. */
    type Container = Option[T]

    // from Attr
    override def data :Option[T] = value(name) match {
      case None => None
      case Some(text) => Some(marsh.unmarshal(text))
    }
  }

  /** Creates an optional attribute. */
  protected def optA[T] (marsh :Marshaler[T], name :Symbol) = new OptAttr[T](marsh, name)

  /** Represents a list attribute. */
  protected class ListAttr[T] (marsh :Marshaler[T], name :Symbol) extends Attr[T](marsh, name) {
    /** List attribute data is wrapped in a List. */
    type Container = List[T]

    def data :List[T] = value(name) match {
      case None => Nil
      case Some(text) => text.split("\t").map(marsh.unmarshal).toList // TODO: better separator?
    }
  }

  /** Creates an attribute for an optional attribute of a database object. */
  protected def listA[T] (marsh :Marshaler[T], name :Symbol) = new ListAttr[T](marsh, name)

  // bring our marshalers into scope
  protected def uuidM = DatabaseObject.uuidM
  protected def booleanM = DatabaseObject.booleanM
  protected def stringM = DatabaseObject.stringM
  protected def intM = DatabaseObject.intM
  protected def dateM = DatabaseObject.dateM
  protected def byteBufferM = DatabaseObject.byteBufferM

  // I don't like this, need to figure out something better
  private[persist] def init (attrs :Map[Symbol, String]) {
    _attrs = attrs
  }

  private[this] var _attrs :Map[Symbol, String] = null
}

object DatabaseObject
{
  class Builder {
    def slurp (props :Properties, prefix :String) :this.type = {
      var pnames = props.propertyNames
      while (pnames.hasMoreElements) {
        val name = pnames.nextElement.asInstanceOf[String]
        if (name.startsWith(prefix)) {
          _map += (Symbol(name.substring(prefix.length)) -> props.getProperty(name))
        }
      }
      this
    }

    protected def add[T <: Any] (name :Symbol, value :T) :this.type = {
      _map += (name -> marsh(value).marshal(value))
      this
    }

    protected def add[T <: Any] (name :Symbol, value :Option[T]) :this.type = {
      value match {
        case None => // nothing doing
        case Some(v) => add(name, v)
      }
      this
    }

    protected def add[T <: Any] (name :Symbol, value :List[T]) :this.type = {
      if (!value.isEmpty) {
        _map += (name -> value.map(marsh(value.head).marshal).mkString("\t"))
      }
      this
    }

    protected def build[T <: DatabaseObject] (obj :T) = {
      _map += ('id -> uuidM.marshal(UUID.randomUUID))
      _map += ('clazz -> obj.getClass.getName)
      obj.init(_map)
      obj
    }

    private[this] def marsh[T <: Any] (value :T) =
      _marshalers.get(value.asInstanceOf[AnyRef].getClass).get.asInstanceOf[Marshaler[T]]

    private[this] var _map = Map[Symbol, String]()
  }

  class Metadata[T <: DatabaseObject] (clazz :Class[T]) {
    /** Returns all attributes of the object that contain UUIDs. */
    val uuidAttrs = clazz.getMethods.filter(m => isType(m, classOf[UUID]))

    /** Returns all attributes of the object that contain Strings. */
    val stringAttrs = clazz.getMethods.filter(m => isType(m, classOf[String]))

    /** Returns all attributes of the object that contain Dates. */
    val dateAttrs = clazz.getMethods.filter(m => isType(m, classOf[Date]))

    private[this] def isType (m :Method, target :Class[_]) = m.getGenericReturnType match {
      case clazz :Class[_] => (clazz == target)
      case ptype :ParameterizedType => (ptype.getActualTypeArguments.length == 1 &&
                                        ptype.getActualTypeArguments.apply(0) == target &&
                                        (ptype.getRawType == classOf[Option[_]] ||
                                         ptype.getRawType == classOf[List[_]]))
      case _ => false
    }
  }

  /** Returns the metadata for the supplied object. */
  def meta[T <: DatabaseObject] (obj :T) :Metadata[T] = meta(obj.getClass.asInstanceOf[Class[T]])

  /** Returns the metadata for objects of the supplied class. */
  // TODO: can we do this with getOrElseUpdate and some type jockeying?
  def meta[T <: DatabaseObject] (clazz :Class[T]) :Metadata[T] = _metadb.get(clazz) match {
    case Some(meta) => meta.asInstanceOf[Metadata[T]]
    case None => {
      val meta :Metadata[T] = new Metadata(clazz)
      _metadb += (clazz -> meta)
      meta
    }
  }

  /** Used to convert strings to and from target data types. */
  protected trait Marshaler[T] {
    /** Marshals a value for this attribute into a string representation. */
    def marshal (value :T) :String

    /** Unmarshals a value for this attribute from its underlying string representation. */
    def unmarshal (value :String) :T
  }

  /** Creates a marshaler for {@link UUID} attrs. */
  protected val uuidM :Marshaler[UUID] = new Marshaler[UUID] {
    def marshal (value :UUID) = value.toString
    def unmarshal (value :String) = UUID.fromString(value)
  }

  /** Creates a marshaler for Boolean attrs. */
  protected val booleanM :Marshaler[Boolean] = new Marshaler[Boolean] {
    def marshal (value :Boolean) = value.toString
    def unmarshal (value :String) = java.lang.Boolean.parseBoolean(value)
  }

  /** Creates a marshaler for String attrs. */
  protected val stringM :Marshaler[String] = new Marshaler[String] {
    def marshal (value :String) = value
    def unmarshal (value :String) = value
  }

  /** Creates a marshaler for Int attrs. */
  protected val intM :Marshaler[Int] = new Marshaler[Int] {
    def marshal (value :Int) = value.toString
    def unmarshal (value :String) = java.lang.Integer.parseInt(value)
  }

  /** Creates a marshaler for {@link Date} attrs. */
  protected val dateM :Marshaler[Date] = new Marshaler[Date] {
    def marshal (value :Date) = value.getTime.toString
    def unmarshal (value :String) = new Date(java.lang.Long.parseLong(value))
  }

  /** Creates a marshaler for {@link ByteBuffer} attrs. */
  protected val byteBufferM :Marshaler[ByteBuffer] = new Marshaler[ByteBuffer] {
    def marshal (value :ByteBuffer) = new String(value.array, "ISO-8859-1")
    def unmarshal (value :String) = ByteBuffer.wrap(value.getBytes("ISO-8859-1"))
  }

  protected val _marshalers = scala.collection.mutable.Map[Class[_], Marshaler[_]]()
  _marshalers += (classOf[UUID] -> uuidM)
  _marshalers += (classOf[Boolean] -> booleanM)
  _marshalers += (classOf[String] -> stringM)
  _marshalers += (classOf[Int] -> intM)
  _marshalers += (classOf[Date] -> dateM)
  _marshalers += (classOf[ByteBuffer] -> byteBufferM)

  protected val _metadb = scala.collection.mutable.Map[Class[_], DatabaseObject.Metadata[_]]()
}
