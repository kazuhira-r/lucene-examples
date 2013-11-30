import scala.collection.JavaConverters._

import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, Sort, SortField, TopDocs}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

import LuceneSorting.AutoCloseableWrapper

object LuceneSorting {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_43

    for (directory <- new RAMDirectory) {
      println("ドキュメント追加")
      createIndex(directory, luceneVersion)

      println("検索開始")
      for (reader <- DirectoryReader.open(directory)) {
        val searcher = new IndexSearcher(reader)

        val query = createQuery("contents:ドキュメント OR contents:1 OR contents: 0", luceneVersion)

        println(s"Query => $query")

        printSearchResult(searcher,
                          "Non Sort",
                          searcher.search(query, null, 20))
        printSearchResult(searcher,
                          "Sort.INDEXORDER",
                          searcher.search(query, 20, Sort.INDEXORDER))
        printSearchResult(searcher,
                          "Sort.RELEVANCE",
                          searcher.search(query, 20, Sort.RELEVANCE))
        printSearchResult(searcher,
                          "Sort(num1 as STRING)",
                          searcher.search(query,
                                          20,
                                          new Sort(new SortField("num1",
                                                                 SortField.Type.STRING))))
        printSearchResult(searcher,
                          "Sort(num1 as INT)",
                          searcher.search(query,
                                          20,
                                          new Sort(new SortField("num1",
                                                                 SortField.Type.INT))))
        try {
          printSearchResult(searcher,
                            "Sort(join-nums as INT)",
                            searcher.search(query,
                                            20,
                                            new Sort(new SortField("join-nums",
                                                                   SortField.Type.INT))))
        } catch {
          case e: NumberFormatException => println(e)
        }
        printSearchResult(searcher,
                          "Sort(num1 as INT, num2 as INT)",
                          searcher.search(query,
                                          20,
                                          new Sort(new SortField("num1",
                                                                 SortField.Type.INT),
                                                   new SortField("num2",
                                                                 SortField.Type.INT))))

        printSearchResult(searcher,
                          "Sort(num1 as INT, num2 as INT#reverse)",
                          searcher.search(query,
                                          20,
                                          new Sort(new SortField("num1",
                                                                 SortField.Type.INT),
                                                   new SortField("num2",
                                                                 SortField.Type.INT,
                                                                 true))))
        printSearchResult(searcher,
                          "Sort(num2#reverse as INT, num1 as INT)",
                          searcher.search(query,
                                          20,
                                          new Sort(new SortField("num2",
                                                                 SortField.Type.INT,
                                                                 true),
                                                   new SortField("num1",
                                                                 SortField.Type.INT))))
        printSearchResult(searcher,
                          "Sort(contents as STRING)",
                          searcher.search(query,
                                          20,
                                          new Sort(new SortField("contents",
                                                                 SortField.Type.STRING))))
      }
    }
  }

  private def createIndex(directory: Directory, luceneVersion: Version): Unit = {
    val config = new IndexWriterConfig(luceneVersion, new JapaneseAnalyzer(luceneVersion))

    new IndexWriter(directory, config).foreach { writer =>
      SampleDocument.createDocs.foreach(writer.addDocument)
    }
  }

  private def createQuery(queryString: String, luceneVersion: Version): Query =
    new QueryParser(luceneVersion,
                    "num1",
                    new JapaneseAnalyzer(luceneVersion))
                      .parse(queryString)

  private def printSearchResult(searcher: IndexSearcher, name: String, docs: TopDocs): Unit = {
    println(s"$name => ヒット件数:${docs.totalHits}")

    val hits = docs.scoreDocs
    hits.take(20).foreach { h =>
      val hitDoc = searcher.doc(h.doc)
      hitDoc
        .getFields
        .asScala
        .map(_.stringValue)
        .mkString(s"  Score,DocNo[${h.score},${h.doc}] ", " | ", "")
        .foreach(print)
      println()
    }
  }

  implicit class AutoCloseableWrapper[A <: AutoCloseable](val underlying: A) extends AnyVal {
    def foreach(body: A => Unit): Unit = {
      try {
        body(underlying)
      } finally {
        if (underlying != null) {
          underlying.close()
        }
      }
    }
  }
}

object SampleDocument {
  def createDocs: List[Document] = {
    List(
      create("30", "30", "30-30", "30-30のドキュメントです"),
      create("1", "1", "1-1", "1-1のドキュメントです"),
      create("1", "90", "1-90", "1-90のドキュメントです"),
      create("2", "2", "2-2", "2-2のドキュメントです"),
      create("2", "50", "2-50", "2-50のドキュメントです"),
      create("20", "20", "20-20", "20-20のドキュメントです"),
      create("10", "10", "10-10", "10-10のドキュメントです")
    )
  }

  private def create(tokens: String*): Document = {
    val doc = new Document
    doc.add(stringField("num1", tokens(0)))
    doc.add(stringField("num2", tokens(1)))
    doc.add(stringField("join-nums", tokens(2)))
    doc.add(textField("contents", tokens(3)))
    doc
  }

  private def stringField(name: String, value: String): Field =
    new StringField(name, value, Field.Store.YES)

  private def textField(name: String, value: String): Field =
    new TextField(name, value, Field.Store.YES)
}
