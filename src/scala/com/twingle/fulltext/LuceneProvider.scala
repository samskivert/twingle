//
// $Id$

package com.twingle.fulltext

import java.io.{File, IOException}
import java.util.UUID

import scala.collection.mutable.ListBuffer

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.index.{IndexReader, IndexWriter}
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.{HitCollector, IndexSearcher, Query}

import com.twingle.Log.log
import com.twingle.persist.DatabaseObject

class LuceneProvider (val indexPath :String) extends FullTextService
{
  // from FullTextService
  def index (obj :DatabaseObject) :Unit = synchronized {
    val meta = DatabaseObject.meta(obj)
    meta.stringAttrs.map(_.invoke(obj).asInstanceOf[String]).foreach(s => index(obj.id, s))
    _writer.commit
  }

  protected def index (id :UUID, text :String) {
    // create the lucene document to be added to the index
    val doc :Document = new Document
    // index terms in the text, but don't store text itself in the lucene index
    doc.add(new Field(TEXT_FIELD, text, Field.Store.NO, Field.Index.ANALYZED))
    // store the twingle unique id, but don't analyze it for the index
    doc.add(new Field(ID_FIELD, id.toString, Field.Store.YES, Field.Index.NOT_ANALYZED))

    // add the document to the index
    _writer.addDocument(doc)
  }

  /**
   * Reusable {@link HitCollector} to process search results as we find them following a full text
   * search on the lucene index.
   */
  class ResultCollector extends HitCollector {
    def collect (docId :Int, score :Float) {
      log.info("Found document", "docId", ""+docId, "score", ""+score)
      _matchDocIds.append(docId)
    }

    def getMatchDocIds :Seq[Int] = _matchDocIds.toList

    private[this] val _matchDocIds = new ListBuffer[Int]
  }

  // from FullTextService
  def search (query :String) :Iterator[UUID] = {
    val parsedQuery = _parser.parse(query)

    // the reader may change, so we need to create a new searcher each time
    val searcher = new IndexSearcher(getIndexReader)

    // gather all matching lucene documents
    val collector = new ResultCollector
    searcher.search(parsedQuery, collector)

    // turn the lucene documents into a list of twingle doc ids
    def uuid (docId :Int) = UUID.fromString(searcher.doc(docId).get(ID_FIELD))
    collector.getMatchDocIds.map(uuid).elements
  }

  protected def getIndexReader :IndexReader = {
    // give the reader a chance to reopen the index if it's changed.  this will return the current
    // reader if no change, else a new reader, so we need to update our cached reader reference
    (_reader = _reader.reopen).asInstanceOf[IndexReader]
  }

  /**
   * Reusable {@link IndexWriter} instance with which we add database objects to the lucene index.
   * Creating these is expensive so we keep one around and just save off any additions with a
   * periodic <tt>commit</tt>.
   */
   private[this] val _writer = new IndexWriter(new File(indexPath), new StandardAnalyzer, true,
                                              IndexWriter.MaxFieldLength.LIMITED)

  /**
   * Reusable {@link IndexReader} instance with which we search the lucene index for documents
   * matching user-specified query terms.  Creating these is expensive so we keep one around and
   * always make sure to call <tt>reopen</tt> before use in case the index has been updated and is
   * in need of reloading.
   */
  private[this] var _reader :IndexReader = IndexReader.open(indexPath)

  /**
   * Reusable {@link QueryParser} to always search the single field we use for performing
   * full text searches.
   */
  private[this] val _parser = new QueryParser("text", new StandardAnalyzer)

  /** The lucene document field name in which we set the twingle document text. */
  private[this] val TEXT_FIELD = "text"

  /** The lucene document field name in which we set the unique twingle document id. */
  private[this] val ID_FIELD = "id"
}
