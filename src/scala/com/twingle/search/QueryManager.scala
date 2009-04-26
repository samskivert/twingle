//
// $Id$

package com.twingle.search

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexReader
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.{HitCollector, IndexSearcher, Query}

import com.twingle.Log.log

/**
 * Simple test app to query a Lucene index and show matching documents.
 */
class QueryManager (val indexPath :String)
{
  class StreamingHitCollector extends HitCollector {
    def collect (doc :Int, score :Float) {
      log.info("Found document", "doc", ""+doc, "score", ""+score)
    }
  }
  
  def query (userQuery :String) {
    val reader = IndexReader.open(indexPath)
    val searcher = new IndexSearcher(reader)
    val analyzer = new StandardAnalyzer

    val field = "contents"
    val parser = new QueryParser(field, analyzer)

    val parsedQuery = parser.parse(userQuery)
    val hitCollector = new StreamingHitCollector

    searcher.search(parsedQuery, hitCollector)
  }
}

object QueryManagerApp
{
  def main (args :Array[String]) {
    val qmgr = new QueryManager("/tmp/corpus-index")
    qmgr.query(args(0))
  }
}
