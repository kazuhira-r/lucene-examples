import scala.collection.JavaConverters._

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.{IndexSearcher, Query, MatchAllDocsQuery, SearcherFactory, SearcherManager, Sort}
import org.apache.lucene.search.{TopFieldCollector, TotalHitCountCollector}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

object LuceneSearcherManager {
  private var currentId: Int = 1

  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_45
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    for {
      directory <- new RAMDirectory
      writer <- new IndexWriter(directory,
                                     new IndexWriterConfig(luceneVersion, analyzer)
                                       .setMaxBufferedDocs(100)
                                       .setRAMBufferSizeMB(100))
    } {
      (1 to 3).foreach(i => registerDocument(writer))
      writer.commit()

      commandWhile(directory, writer)
    }
  }

  private def commandWhile(directory: Directory, writer: IndexWriter): Unit = {
    val searcherManager = new SearcherManager(writer, true, new SearcherFactory)
    //val searcherManager = new SearcherManager(directory, new SearcherFactory)

    Iterator
      .continually(readLine("Command> "))
      .withFilter(l => l != null && !l.isEmpty)
      .takeWhile(_ != "exit")
      .foreach {
        case "add" =>
          registerDocument(writer)
          println(s"Ducument added[${currentId - 1}]")
        case "search" =>
          val searcher = searcherManager.acquire

          val query = new MatchAllDocsQuery

          val totalHitCountCollector = new TotalHitCountCollector
          searcher.search(query, totalHitCountCollector)
          println(s"Total Document => ${totalHitCountCollector.getTotalHits}")

          val docCollector = TopFieldCollector.create(Sort.RELEVANCE,
                                                      100,
                                                      true,
                                                      false,
                                                      false,
                                                      false)
          searcher.search(query, docCollector)

          for (h <- docCollector.topDocs.scoreDocs) {
            val hitDoc = searcher.doc(h.doc)
            println(s"Score,ID[${h.score}:${h.doc}]: Doc => " +
                    hitDoc
                      .getFields
                      .asScala
                      .map(_.stringValue)
                      .mkString(" | "))
          }

          searcherManager.release(searcher)
        case "commit" =>
          writer.commit()
          println("IndexWriter committed.")
        case "refresh" =>
          searcherManager.maybeRefresh()
          println("SearcherManager maybeRefreshed.")
        case command => println(s"Unknown Command, [$command]")
      }
  }

  private def registerDocument(writer: IndexWriter): Unit = {
    val document = new Document
    document.add(new StringField("id", currentId.toString, Field.Store.YES))
    document.add(new StringField("contents", "contents-%s".format(currentId), Field.Store.YES))
    writer.addDocument(document)

    currentId += 1
  }

  implicit class AutoCloseableWrapper[A <: AutoCloseable](val underlying: A) extends AnyVal {
    def foreach(fun: A => Unit): Unit =
      try {
        fun(underlying)
      } finally {
        underlying.close()
      }
  }
}
