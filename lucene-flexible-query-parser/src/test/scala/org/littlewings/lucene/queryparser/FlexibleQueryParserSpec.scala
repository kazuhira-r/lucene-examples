package org.littlewings.lucene.queryparser

import java.text.DecimalFormat

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document._
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.ConfigurationKeys
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.scalatest.{FunSuite, Matchers}

class FlexibleQueryParserSpec extends FunSuite with Matchers {
  val bookDocuments: Array[Document] = Array(
    createBookDocument("978-4774127804", "Apache Lucene 入門 ~Java・オープンソース・全文検索システムの構築", 2270),
    createBookDocument("978-4774189307", "[改訂第3版]Apache Solr入門――オープンソース全文検索エンジン", 4104),
    createBookDocument("978-4048662024", "高速スケーラブル検索エンジン ElasticSearch Server", 6642),
    createBookDocument("978-4774167534", "検索エンジン自作入門 ～手を動かしながら見渡す検索の舞台裏", 2894),
    createBookDocument("978-4822284619", "検索エンジンはなぜ見つけるのか", 2592)
  )

  test("classic query parser") {
    withDirectory(new RAMDirectory) { directory =>
      val analyzer = new JapaneseAnalyzer
      writeDocuments(directory, bookDocuments, analyzer)

      val searcherManager = new SearcherManager(directory, new SearcherFactory)
      val indexSearcher = searcherManager.acquire()
      try {
        val query = new QueryParser("isbn", analyzer).parse("title: 全文検索 AND title: 入門")
        val topDocs = indexSearcher.search(query, 3, new Sort(new SortField("price", SortField.Type.INT, true)))

        val scoreDocs = topDocs.scoreDocs
        scoreDocs should have size (3)

        indexSearcher.doc(scoreDocs(0).doc).get("title") should be("[改訂第3版]Apache Solr入門――オープンソース全文検索エンジン")
        indexSearcher.doc(scoreDocs(1).doc).get("title") should be("検索エンジン自作入門 ～手を動かしながら見渡す検索の舞台裏")
        indexSearcher.doc(scoreDocs(2).doc).get("title") should be("Apache Lucene 入門 ~Java・オープンソース・全文検索システムの構築")

        query should be(a[BooleanQuery])
        query.toString should be("+(title:全文 title:検索) +title:入門")
      } finally {
        searcherManager.release(indexSearcher)
      }
    }
  }

  test("classic query parser, with numeric") {
    withDirectory(new RAMDirectory) { directory =>
      val analyzer = new JapaneseAnalyzer
      writeDocuments(directory, bookDocuments, analyzer)

      val searcherManager = new SearcherManager(directory, new SearcherFactory)
      val indexSearcher = searcherManager.acquire()
      try {
        val query = new QueryParser("isbn", analyzer).parse("price: [2000 TO 3000]")
        val topDocs = indexSearcher.search(query, 3, new Sort(new SortField("price", SortField.Type.INT, true)))

        val scoreDocs = topDocs.scoreDocs
        scoreDocs should be(empty)

        query should be(a[TermRangeQuery])
      } finally {
        searcherManager.release(indexSearcher)
      }
    }
  }

  test("standard query parser") {
    withDirectory(new RAMDirectory) { directory =>
      val analyzer = new JapaneseAnalyzer
      writeDocuments(directory, bookDocuments, analyzer)

      val searcherManager = new SearcherManager(directory, new SearcherFactory)
      val indexSearcher = searcherManager.acquire()
      try {
        val query = new StandardQueryParser(analyzer).parse("title: 全文検索 AND title: 入門", "isbn")
        val topDocs = indexSearcher.search(query, 3, new Sort(new SortField("price", SortField.Type.INT, true)))

        val scoreDocs = topDocs.scoreDocs
        scoreDocs should have size (3)

        indexSearcher.doc(scoreDocs(0).doc).get("title") should be("[改訂第3版]Apache Solr入門――オープンソース全文検索エンジン")
        indexSearcher.doc(scoreDocs(1).doc).get("title") should be("検索エンジン自作入門 ～手を動かしながら見渡す検索の舞台裏")
        indexSearcher.doc(scoreDocs(2).doc).get("title") should be("Apache Lucene 入門 ~Java・オープンソース・全文検索システムの構築")

        query should be(a[BooleanQuery])
        query.toString should be("+(title:全文 title:検索) +title:入門")
      } finally {
        searcherManager.release(indexSearcher)
      }
    }
  }

  test("standard query parser, non analyzer") {
    val query = new StandardQueryParser().parse("title: 全文検索 AND title: 入門 AND title:SPRING", "isbn")

    query should be(a[BooleanQuery])
    query.toString should be("+title:全文検索 +title:入門 +title:SPRING")
  }

  test("standard query parser, with numeric") {
    withDirectory(new RAMDirectory) { directory =>
      val analyzer = new JapaneseAnalyzer
      writeDocuments(directory, bookDocuments, analyzer)

      val searcherManager = new SearcherManager(directory, new SearcherFactory)
      val indexSearcher = searcherManager.acquire()
      try {
        val query = new StandardQueryParser(analyzer).parse("price: [2000 TO 3000]", "isbn")
        val topDocs = indexSearcher.search(query, 3, new Sort(new SortField("price", SortField.Type.INT, true)))

        val scoreDocs = topDocs.scoreDocs
        scoreDocs should be(empty)

        query should be(a[TermRangeQuery])
      } finally {
        searcherManager.release(indexSearcher)
      }
    }
  }

  test("standard query parser, with numeric, with point-config") {
    withDirectory(new RAMDirectory) { directory =>
      val analyzer = new JapaneseAnalyzer
      writeDocuments(directory, bookDocuments, analyzer)

      val searcherManager = new SearcherManager(directory, new SearcherFactory)
      val indexSearcher = searcherManager.acquire()
      try {
        val queryParser = new StandardQueryParser(analyzer)
        queryParser
          .getQueryConfigHandler
          .addFieldConfigListener(fieldConfig =>
            if (fieldConfig.getField == "price") {
              fieldConfig.set(
                ConfigurationKeys.POINTS_CONFIG,
                new PointsConfig(new DecimalFormat("###"), classOf[Integer])
              )
            }
          )
        val query = queryParser.parse("price: [2000 TO 3000]", "isbn")
        val topDocs = indexSearcher.search(query, 3, new Sort(new SortField("price", SortField.Type.INT, true)))

        val scoreDocs = topDocs.scoreDocs
        scoreDocs should have size (3)

        indexSearcher.doc(scoreDocs(0).doc).get("title") should be("検索エンジン自作入門 ～手を動かしながら見渡す検索の舞台裏")
        indexSearcher.doc(scoreDocs(1).doc).get("title") should be("検索エンジンはなぜ見つけるのか")
        indexSearcher.doc(scoreDocs(2).doc).get("title") should be("Apache Lucene 入門 ~Java・オープンソース・全文検索システムの構築")

        query should be(a[PointRangeQuery])
      } finally {
        searcherManager.release(indexSearcher)
      }
    }
  }

  protected def withDirectory(directory: Directory)(fun: Directory => Unit): Unit = {
    try {
      fun(directory)
    } finally {
      directory.close()
    }
  }

  protected def writeDocuments(directory: Directory, documents: Seq[Document], analyzer: Analyzer = new StandardAnalyzer): Unit = {
    val indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))
    try {
      documents.foreach(indexWriter.addDocument)
      indexWriter.commit()
    } catch {
      case e: Exception =>
        indexWriter.rollback()
        throw e
    } finally {
      indexWriter.close()
    }
  }

  protected def createBookDocument(isbn: String, title: String, price: Int): Document = {
    val document = new Document

    document.add(new StringField("isbn", isbn, Store.YES))
    document.add(new TextField("title", title, Store.YES))
    document.add(new IntPoint("price", price))
    document.add(new NumericDocValuesField("price", price))

    document
  }
}
