//
// $Id$

package com.twingle.spider

import java.net.URLEncoder

import scala.collection.mutable.ListBuffer

class QueryParamBuilder
{
  def add[T] (key :String, value :Option[T]) :QueryParamBuilder = {
    if (value.isDefined) { _params.append(( key, value.get.toString )) }; this
  }

  def build :String = {
    _params.map(tup => {
      val b = new StringBuilder
      b.append(URLEncoder.encode(tup._1))
      b.append('=')
      b.append(URLEncoder.encode(tup._2))
      b.toString
    }).mkString("", "&", "")
  }

  protected[this] val _params :ListBuffer[(String, String)] = new ListBuffer[(String, String)]
}

package tests {
  import org.scalatest.Suite

  class QueryParamBuilderSuite extends Suite {
    def testOneParam () {
      val builder = new QueryParamBuilder
      builder.add("food", Some("burrito"))
      val params = builder.build

      expect("food=burrito") { params }
    }

    def testTwoParams () {
      val builder = new QueryParamBuilder
      builder.add("food", Some("burrito"))
      builder.add("drink", Some("coke"))
      val params = builder.build

      expect("food=burrito&drink=coke") { params }
    }

    def testNoneParam () {
      val builder = new QueryParamBuilder
      builder.add("food", None)
      val params = builder.build

      expect("") { params }
    }

    def testSomeWithNoneParam () {
      val builder = new QueryParamBuilder
      builder.add("food", Some("burrito"))
      builder.add("drink", None)
      val params = builder.build

      expect("food=burrito") { params }
    }

    def testEncodedParam () {
      val builder = new QueryParamBuilder
      builder.add("drink", Some("anchor steam"))
      builder.add("game", Some("jak&daxter"))
      val params = builder.build

      expect("drink=anchor+steam&game=jak%26daxter") { params }
    }
  }
}
