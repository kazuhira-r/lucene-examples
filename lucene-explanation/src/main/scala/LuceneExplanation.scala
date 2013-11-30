import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, Sort, SortField, TopDocs}
import org.apache.lucene.search.{TopFieldCollector, TotalHitCountCollector, TopScoreDocCollector}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

import LuceneExplanation.AutoCloseableWrapper

object LuceneExplanation {
  type Store = Boolean
  type Analyze = Boolean

  def main(args: Array[String]): Unit = {
    val directory = new RAMDirectory
    val luceneVersion = Version.LUCENE_44
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    registerDocuments(directory, luceneVersion, analyzer)

    InteractiveQuery(directory, luceneVersion).queryWhile("title", analyzer)
  }

  private def registerDocuments(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit = {
    for (indexWriter <- new IndexWriter(directory,
                                        new IndexWriterConfig(luceneVersion, analyzer))) {
      indexWriter.addDocument(createDocument(Map("isbn-13" -> ("978-4894714991", false, true),
                                                 "publish-date" -> ("2008-11-27", false, true),
                                                 "title" -> ("Effective Java 第2版 (The Java Series)", true, true),
                                                 "price" -> ("3780", false, true),
                                                 "abstract" -> ("Javaプログラミング書籍の定本「Effective Java」の改訂版です。著者のGoogle, Sun Microsystemsにおけるソフトウェア開発で得た知識・経験をまとめた、JavaでプログラミングをするすべてのSE必読の書籍です。2001年の初版以降の追加項目、JavaSE6.0に対応。", true, true))))
      indexWriter.addDocument(createDocument(Map("isbn-13" -> ("978-4844330844", false, true),
                                                 "publish-date" -> ("2011-09-27", false, true),
                                                 "title" -> ("Scalaスケーラブルプログラミング第2版", true, true),
                                                 "price" -> ("4830", false, true),
                                                 "abstract" -> ("言語設計者自ら、その手法と思想を説く。Scalaプログラミングバイブル!", true, true))))
      indexWriter.addDocument(createDocument(Map("isbn-13" -> ("978-4774147277", false, true),
                                                 "publish-date" -> ("2011-07-06", false, true),
                                                 "title" -> ("プログラミングGROOVY", true, true),
                                                 "price" -> ("3360", false, true),
                                                 "abstract" -> ("GroovyはJavaと抜群の親和性を持つハイブリッド言語です。簡潔で強力な記述力と高い柔軟性を持っており、Javaを補完・強化する究極のパートナーになります。", true, true))))
      indexWriter.addDocument(createDocument(Map("isbn-13" -> ("978-4274069130", false, true),
                                                 "publish-date" -> ("2013-04-26", false, true),
                                                 "title" -> ("プログラミングClojure 第2版", true, true),
                                                 "price" -> ("3570", false, true),
                                                 "abstract" -> ("プログラミング言語Clojureの実践的な解説書の改訂2版!", true, true))))
      indexWriter.addDocument(createDocument(Map("isbn-13" -> ("978-4774127804", false, true),
                                                 "publish-date" -> ("2006-05-17", false, true),
                                                 "title" -> ("Apache Lucene 入門 ～Java・オープンソース・全文検索システムの構築", true, true),
                                                 "price" -> ("3360", false, true),
                                                 "abstract" -> ("Luceneは全文検索システムを構築するためのJavaのライブラリです。Luceneを使えば,一味違う高機能なWebアプリケーションを作ることができます。", true, true))))
     }
  }

  private def createDocument(source: Map[String, (String, Analyze, Store)]): Document = {
    val document = new Document

    for ((fieldName, (value, isAnalyze, isStore)) <- source) {
      val store =
        if (isStore) Field.Store.YES
        else Field.Store.NO

      if (isAnalyze) {
        document.add(new TextField(fieldName, value, store))
      } else {
        document.add(new StringField(fieldName, value, store))
      }
    }

    document
  }

  implicit class AutoCloseableWrapper[A <: AutoCloseable](val underlying: A) extends AnyVal {
    def foreach(fun: A => Unit): Unit =
      try {
        fun(underlying)
      } finally {
        if (underlying != null) {
          underlying.close()
        }
      }
  }
}

object InteractiveQuery {
  def apply(directory: Directory, luceneVersion: Version): InteractiveQuery =
    new InteractiveQuery(directory, luceneVersion)
}

class InteractiveQuery private(directory: Directory, luceneVersion: Version) {
  val max: Int = 10000

  def queryWhile(defaultField: String, analyzer: Analyzer): Unit = {
    println("Start Interactive Query")

    val queryParser =
      new QueryParser(luceneVersion, defaultField, analyzer)

    val query = (queryString: String) =>
    Try { queryParser.parse(queryString) }.recoverWith {
      case th =>
        println(s"[ERROR] Invalid Query: $th")
        Failure(th)
    }.toOption

    val executeQuery = (searcher: IndexSearcher, queryString: String) => {
      query(queryString).foreach { q =>
        println(s"入力したクエリ => $q")
        println("=====")

        val totalHitCountCollector = new TotalHitCountCollector
        searcher.search(q, totalHitCountCollector)
        val totalHits = totalHitCountCollector.getTotalHits

        println(s"${totalHits}件、ヒットしました")
        println("=====")

        val docCollector =
          //TopScoreDocCollector.create(max, true)
          TopFieldCollector.create(new Sort(new SortField("publish-date",
                                                          SortField.Type.STRING,
                                                          true),
                                            new SortField("price",
                                                          SortField.Type.INT,
                                                          true)),
                                   max,
                                   true,
                                   true,
                                   true,
                                   true)
        searcher.search(q, docCollector)

        val topDocs = docCollector.topDocs
        val hits = topDocs.scoreDocs

        for (h <- hits) {
          val hitDoc = searcher.doc(h.doc)

          val explanation = searcher.explain(q, h.doc)

          println(s"Score,N[${h.score}:${h.doc}] : Doc => " +
                  hitDoc
                    .getFields
                    .asScala
                    .map(_.stringValue)
                    .mkString(" ", " | ", ""))
          println()
          println("Explanation As String => ")
          explanation.toString.lines.map("    " + _).foreach(println)
          println()
          println("Explanation As HTML => ")
          explanation.toHtml.lines.map("    " + _).foreach(println)
          println("---------------")
        }
      }
    }

    for (reader <- DirectoryReader.open(directory)) {
      val searcher = new IndexSearcher(reader)

      Iterator
        .continually(readLine("Lucene Query> "))
        .takeWhile(_ != "exit")
        .withFilter(line => !line.isEmpty && line != "\\c")
        .foreach(line => executeQuery(searcher, line))
    }
  }
}
