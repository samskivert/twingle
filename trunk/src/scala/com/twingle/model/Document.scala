//
// $Id$

package com.twingle.model

import java.nio.ByteBuffer
import java.util.Date

import com.twingle.persist.DatabaseObject

/**
 * Contains data for a Document stored in the database.
 */
class Document extends AnyRef with DatabaseObject
{
  /** The URL defining this document's location. */
  def location :Option[String] = optA(stringM, "location").data

  /** The name of this document. */
  def name :Option[String] = optA(stringM, "name").data

  /** The text of this document. */
  def text :Option[String] = optA(stringM, "text").data

  /** This document's binary data. */
  def bits :Option[ByteBuffer] = optA(byteBufferM, "bits").data

  /** The date on which this document was created. */
  def created :Option[Date] = optA(dateM, "created").data

  /** The date on which this document was last modified. */
  def lastModified :Option[Date] = optA(dateM, "last_modified").data
}

/**
 * Document utility methods.
 */
object Document
{
  class Builder extends DatabaseObject.Builder {
    def location (location :String) :this.type = add("location", location)
    def name (name :String) :this.type = add("name", name)
    def text (text :String) :this.type = add("text", text)
    def bits (bits :ByteBuffer) :this.type = add("bits", bits)
    def created (created :Date) :this.type = add("created", created)
    def lastModified (lastModified :Date) :this.type = add("last_modified", lastModified)
  }
  def builder = new Builder {
    def build :Document = build(new Document)
  }
}

package tests {
  import org.scalatest.Suite

  class DocumentSuite extends Suite {
    def testBuilder () {
      val now = new Date
      val doc = Document.builder.
        location("http://www.samskivert.com/").
        name("MDB's Blog").
        text("Foo bar baz!").
        created(now).
        lastModified(now).
        build

      expect(Some("http://www.samskivert.com/")) { doc.location }
      expect(Some("MDB's Blog")) { doc.name }
      expect(Some("Foo bar baz!")) { doc.text }
      expect(None) { doc.bits }
      expect(Some(now)) { doc.created }
      expect(Some(now)) { doc.lastModified }
    }
  }
}
