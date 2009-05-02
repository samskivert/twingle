//
// $Id$

package com.twingle.model

import java.util.UUID
import java.util.Date

/**
 * Contains data for a Message stored in the database.
 */
class Message extends Document
{
  /** The subject of this message (an alias for {@link Document#name}. */
  def subject :Option[String] = name

  /** The author of this message. */
  def author :UUID = reqA(uuidM, "author").data

  /** The recipients of this message. */
  def recipients :List[UUID] = listA(uuidM, "recipients").data

  /** The id of the conversation of which this message is a part. */
  def conversation :Option[UUID] = optA(uuidM, "conversation").data
}

/**
 * Message utility methods.
 */
object Message
{
  def builder = new Document.Builder {
    def subject (subject :String) = name(subject) // subject is an alias for "name"
    def author (author :UUID) = add("author", author)
    def recipients (recipients :List[UUID]) = add("recipients", recipients)
    def conversation (conversation :UUID) = add("conversation", conversation)
    def build :Message = build(new Message)
  }
}

package tests {
  import org.scalatest.Suite

  class MessageSuite extends Suite {
    def testBuilder () {
      val now = new Date
      val mdb = Person.builder.
        name("Michael Bayne").
        twitter("samskivert").
        build
      val shaper = Person.builder.
        name("Walter Korman").
        twitter("shaper").
        build
      val tweet = Message.builder.
        location("twitter://1231232").
        name("Tweet tweet").
        text("@shaper Lulz!").
        created(now).
        lastModified(now).
        author(mdb.id).
        recipients(List(shaper.id)).
        build

      expect(Some("twitter://1231232")) { tweet.location }
      expect(Some("Tweet tweet")) { tweet.name }
      expect(mdb.id) { tweet.author }
      expect(List(shaper.id)) { tweet.recipients }

      val email = Message.builder.
        location("49F3AECF.10406@threerings.net").
        subject("hourly panopticons").
        text("Blah blah teleblah").
        created(now).
        lastModified(now).
        author(mdb.id).
        recipients(List(shaper.id, mdb.id)).
        build

      expect(List(shaper.id, mdb.id)) { email.recipients }
    }
  }
}
