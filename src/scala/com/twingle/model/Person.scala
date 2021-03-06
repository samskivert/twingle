//
// $Id$

package com.twingle.model

import com.twingle.persist.DatabaseObject

/**
 * Contains data for a Person stored in the database.
 */
class Person extends AnyRef with DatabaseObject
{
  /** This person's primary name. */
  def name () :Option[String] = optA(stringM, 'name).data

  /** Other names by which this person is known. */
  def aliases () :List[String] = listA(stringM, 'aliases).data

  /** Email addresses associated with this person. */
  def addresses () :List[String] = listA(stringM, 'addresses).data

  /** This person's Twitter account id. */
  def twitter () :Option[String] = optA(stringM, 'twitter).data

  /** This person's FriendFeed account id. */
  def friendfeed () :Option[String] = optA(stringM, 'friendfeed).data
}

/**
 * Person utility methods.
 */
object Person
{
  def builder = new DatabaseObject.Builder {
    def name (name :String) = add('name, name)
    def aliases (aliases :List[String]) = add('aliases, aliases)
    def addresses (addresses :List[String]) = add('addresses, addresses)
    def twitter (twitter :String) = add('twitter, twitter)
    def friendfeed (friendfeed :String) = add('friendfeed, friendfeed)
    def build :Person = build(new Person)
  }
  // TODO: how to expand an existing record?
}
