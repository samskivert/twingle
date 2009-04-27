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

/**
 * Provides full text search services for Twingle {@link DatabaseObject} records using the Lucene
 * search engine.
 *
 * @param indexPath full path to the directory under which the Lucene search index data is
 *                  to be stored.  An initially empty index will be created if one does not
 *                  already exist.
 */
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
  def search (query :String) :Iterator[UUID] = synchronized {
    val parsedQuery = _parser.parse(query)

    // for performance reasons we reuse the reader and searcher as much as possible, but before
    // using we have to give the reader a chance to reopen indexes in case they've changed, and if
    // the reader changes, then the searcher has to change as well.  if we needed to un-synchronize
    // this method for even more performance purposes, we'd have to rework this to either not cache
    // the reader and searcher, or use pools, or something else i'm not bothering to think of right
    // now.
    refreshSearcher()

    // gather all matching lucene documents
    val collector = new ResultCollector
    _searcher.search(parsedQuery, collector)

    // turn the lucene documents into a list of twingle doc ids
    def uuid (docId :Int) = UUID.fromString(_searcher.doc(docId).get(ID_FIELD))
    collector.getMatchDocIds.map(uuid).elements
  }

  protected def refreshSearcher () {
    // save off a reference to our current reader so we can see if it changes subsequently
    val origReader = _reader

    // per lucene's somewhat wacky api, we give the reader a chance to reopen the index file which
    // if necessary means it will return a new reader object, else it will return itself
    val newReader = _reader.reopen

    if (newReader != origReader) {
      // the indexes must have changed, so save off our new reader
      _reader = newReader

      // and create a new searcher referencing the new reader
      _searcher = new IndexSearcher(_reader)
    }
  }

  /**
   * Reusable {@link IndexWriter} instance with which we add database objects to the lucene index.
   * Creating these is expensive so we keep one around and just save off any additions with a
   * periodic <tt>commit</tt>.
   */
   private[this] val _writer = new IndexWriter(new File(indexPath), new StandardAnalyzer, true,
                                              IndexWriter.MaxFieldLength.LIMITED)

  /**
   * Reusable {@link IndexReader} instance with which we read the lucene index of Twingle document
   * text.  Creating these is expensive so we keep one around and always make sure to call
   * <tt>reopen</tt> before use in case the index has been updated and is in need of reloading.
   */
  private[this] var _reader :IndexReader = IndexReader.open(indexPath)

  /**
   * Reusable {@link IndexSearcher} instance with which we search the lucene index for documents
   * matching user-specified query terms.  Creating these is expensive so we keep one around and
   * make sure to recreate it if we ever have to create a new {@link IndexReader}.
   */
  private[this] var _searcher :IndexSearcher = new IndexSearcher(_reader)

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
