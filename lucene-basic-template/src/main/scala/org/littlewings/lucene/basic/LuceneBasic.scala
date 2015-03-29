package org.littlewings.lucene.basic

import scala.io.StdIn
import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, Sort, SortField}
import org.apache.lucene.search.{ScoreDoc, TopDocs, TopFieldCollector, TotalHitCountCollector, TopScoreDocCollector}
import org.apache.lucene.store.{Directory, RAMDirectory}

object LuceneBasic {
  def main(args: Array[String]): Unit = {
    val analyzer = createAnalyzer

    for (directory <- new RAMDirectory) {
      registryDocuments(directory, analyzer)

      queryWhile(directory, analyzer)
    }
  }

  private def createAnalyzer: Analyzer =
    new JapaneseAnalyzer

  private def registryDocuments(directory: Directory, analyzer: Analyzer): Unit =
    for (writer <- new IndexWriter(directory,
      new IndexWriterConfig(analyzer))) {
      Array(
        createDocument {
          Map("isbn" -> "978-4774127804",
            "title" -> "Apache Lucene 入門 ～Java・オープンソース・全文検索システムの構築",
            "price" -> 3360,
            "summary" -> "Luceneは全文検索システムを構築するためのJavaのライブラリです。")
        },
        createDocument {
          Map("isbn" -> "978-4774161631",
            "title" -> "[改訂新版] Apache Solr入門 オープンソース全文検索エンジン",
            "price" -> 3780,
            "summary" -> "最新版Apaceh Solr Ver.4.5.1に対応するため大幅な書き直しと原稿の追加を行い、現在の開発環境に合わせて完全にアップデートしました。Apache Solrは多様なプログラミング言語に対応した全文検索エンジンです。")
        },
        createDocument {
          Map("isbn" -> "978-4797352009",
            "title" -> "集合知イン・アクション",
            "price" -> 3990,
            "summary" -> "レコメンデーションエンジンをつくるには?ブログやSNSのテキスト分析、ユーザー嗜好の予測モデル、レコメンデーションエンジン……Web 2.0の鍵「集合知」をJavaで実装しよう!")
        }
      ).foreach(writer.addDocument)

      writer.commit()
    }

  private def createDocument(entry: Map[String, Any]): Document = {
    val document = new Document
    document.add(new StringField("isbn", entry("isbn").toString, Field.Store.YES))
    document.add(new TextField("title", entry("title").toString, Field.Store.YES))
    document.add(new StringField("price", entry("price").toString, Field.Store.YES))
    document.add(new TextField("summary", entry("summary").toString, Field.Store.YES))
    document
  }

  private def queryWhile(directory: Directory, analyzer: Analyzer): Unit =
    for (reader <- DirectoryReader.open(directory)) {
      val searcher = new IndexSearcher(reader)
      val queryParser = new QueryParser("title", analyzer)
      val limit = 1000

      def parseQuery(queryString: String): Try[Query] =
        Try(queryParser.parse(queryString)).recoverWith {
          case e =>
            println(s"Invalid Query[$queryString], Reason: $e")
            Failure(e)
        }

      def search(query: Query): (Int, Array[ScoreDoc]) = {
        println(s"  Input Query => [$query]")

        val totalHitCountCollector = new TotalHitCountCollector
        searcher.search(query, totalHitCountCollector)
        val totalHits = totalHitCountCollector.getTotalHits

        val docCollector =
          TopFieldCollector.create(Sort.RELEVANCE,
            limit,
            true,  // fillFields
            false,  // trackDocScores
            false)  // traxMaxScore
        searcher.search(query, docCollector)
        val topDocs = docCollector.topDocs
        val hits = topDocs.scoreDocs

        (totalHits, hits)
      }

      Iterator
        .continually(StdIn.readLine("Query> "))
        .takeWhile(_ != "exit")
        .withFilter(line => line != null && !line.isEmpty)
        .map(parseQuery)
        .withFilter(_.isSuccess)
        .map(query => search(query.get))
        .foreach { case (totalHits, hits) =>
          if (totalHits > 0) {
            println(s"  ${totalHits}件ヒットしました")

            hits.foreach { h =>
              val hitDoc = searcher.doc(h.doc)
              println(s"   ScoreDoc, id[${h.score}:${h.doc}]: Doc => " +
                      hitDoc
                        .getFields
                        .asScala
                        .map(_.stringValue)
                        .mkString("|"))
            }
          } else {
            println("ヒット件数は0です")
          }

          println()
        }
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
