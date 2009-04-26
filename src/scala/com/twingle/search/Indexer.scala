//
// $Id$

package com.twingle.search

import java.io.{File, FileReader, IOException}

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{DateTools, Document, Field}
import org.apache.lucene.index.IndexWriter

import com.twingle.Log.log

/**
 * Simple test indexing app ported across from lucene demos.
 */
class Indexer
{
  def index (docPath :String, indexPath :String) {
    try {
      val start = System.currentTimeMillis

      val indexDir = new File(indexPath)
      val writer :IndexWriter = new IndexWriter(indexDir, new StandardAnalyzer, true,
                                                IndexWriter.MaxFieldLength.LIMITED)

      val docDir = new File(docPath)

      log.info("Indexing", "indexDir", indexDir, "docDir", docDir)
      indexDocs(writer, docDir)

      log.info("Optimizing.")

      writer.optimize
      writer.close

      val end = System.currentTimeMillis
      log.info("Finished", "elapsed", ""+(end - start))

    } catch {
      case ioe :IOException => log.warning("Failure indexing.", ioe)
    }
  }

  protected def indexDocs (writer :IndexWriter, file :File) {
    if (file.canRead) {
      if (file.isDirectory) {
        val files = file.list
        if (files != null) {
          files.foreach(path => indexDocs(writer, new File(file, path)))
        }

      } else {
        log.info("Adding", "file", file)
        writer.addDocument(createDocumentFromFile(file))
      }
    }
  }

  protected def createDocumentFromFile (file :File) :Document = {
    val doc :Document = new Document

    doc.add(new Field("path", file.getPath, Field.Store.YES, Field.Index.NOT_ANALYZED))
    doc.add(new Field("modified",
                      DateTools.timeToString(file.lastModified, DateTools.Resolution.MINUTE),
                      Field.Store.YES, Field.Index.NOT_ANALYZED))
    doc.add(new Field("contents", new FileReader(file)))

    doc
  }
}

object IndexerApp
{
  def main (args :Array[String]) {
    val indexer = new Indexer
    indexer.index("/tmp/corpus", "/tmp/corpus-index")
  }
}
