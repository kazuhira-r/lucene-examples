package org.littlewings.lucene.multifield

import scala.collection.JavaConverters._

import org.apache.lucene.analysis.{Analyzer, AnalyzerWrapper}
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, Sort, SortField, TopFieldCollector}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

object LuceneMultiField {
  def main(args: Array[String]): Unit = {
    val version = Version.LUCENE_CURRENT
    val analyzer = createAnalyzer(version)
    val queryAnalyzer = createQueryAnalyzer(version)

    for (directory <- new RAMDirectory) {
      registerDocuments(directory, version, analyzer)

      executeQuery(createQuery("*:*", version, queryAnalyzer),
                   Sort.RELEVANCE,
                   directory)
      executeQuery(createQuery("title:Lucene title:オープンソース", version, queryAnalyzer),
                   new Sort(new SortField("title", SortField.Type.STRING, false)),
                   directory)

      executeQuery(createQuery("tags:Lucene", version, queryAnalyzer),
                   Sort.RELEVANCE,
                   directory)

      executeQuery(createQuery("tags:Lucene", version, queryAnalyzer),
                   new Sort(new SortField("tags", SortField.Type.STRING, true)),
                   directory)

      executeQuery(createQuery("tags:Lucene +tags:Elasticsearch", version, queryAnalyzer),
                   Sort.RELEVANCE,
                   directory)

      executeQuery(createQuery("authors:株式会社 authors:関口", version, queryAnalyzer),
                   Sort.RELEVANCE,
                   directory)

      executeQuery(createQuery("authors:株式会社 authors:ロンウイット", version, queryAnalyzer),
                   new Sort(new SortField("authors", SortField.Type.STRING, true)),
                   directory)
    }
  }

  private def createAnalyzer(version: Version): Analyzer =
    new JapaneseAnalyzer(version)

  private def createQueryAnalyzer(version: Version): Analyzer =
    new AnalyzerWrapper(Analyzer.PER_FIELD_REUSE_STRATEGY) {
      override def getWrappedAnalyzer(fieldName: String): Analyzer =
        fieldName match {
          case "isbn" => new KeywordAnalyzer
          case "title" => createAnalyzer(version)
          case "tags" => new KeywordAnalyzer
          case "authors" => createAnalyzer(version)
        }
    }

  private def registerDocuments(directory: Directory, version: Version, analyzer: Analyzer): Unit =
    for (writer <- new IndexWriter(directory,
                                   new IndexWriterConfig(version, analyzer))) {
      Array(
        createDocument(Map("isbn" -> "978-4774127804",
                           "title" -> "Apache Lucene 入門 ～Java・オープンソース・全文検索システムの構築",
                           "tags" -> Seq("Java", "Lucene", "全文検索", "オープンソース"),
                           "authors" -> Seq("関口 宏司"))),
        createDocument(Map("isbn" -> "978-4774161631",
                           "title" -> "[改訂新版] Apache Solr入門 オープンソース全文検索エンジン",
                           "tags" -> Seq("Java", "Lucene", "Solr", "全文検索", "オープンソース"),
                           "authors" -> Seq("大谷 純", "阿部 慎一朗", "大須賀 稔", "北野 太郎", "鈴木 教嗣", "平賀 一昭", "株式会社リクルートテクノロジーズ", "株式会社ロンウイット"))),
        createDocument(Map("isbn" -> "978-4048662024",
                          "title" -> "高速スケーラブル検索エンジン ElasticSearch Server",
                          "tags" -> Seq("Java", "Elasticsearch", "全文検索", "オープンソース"),
                          "authors" -> Seq("Rafal Kuc", "Marek Rogozinski", "株式会社リクルートテクノロジーズ", "大岩 達也", "大谷 純", "兼山 元太", "水戸 祐介", "守谷 純之介")))
      ).foreach(writer.addDocument)

      writer.commit()
    }

  private def createDocument(entry: Map[String, Any]): Document = {
    val document = new Document
    document.add(new StringField("isbn", entry("isbn").toString, Field.Store.YES))
    document.add(new TextField("title", entry("title").toString, Field.Store.YES))

    for {
      Seq(tags @ _*) <- entry.get("tags")
      tag <- tags
    } {
      document.add(new StringField("tags", tag.toString, Field.Store.YES))
    }

    for {
      Seq(authors @ _*) <- entry.get("authors")
      author <- authors
    } {
      document.add(new TextField("authors", author.toString, Field.Store.YES))
    }

    document
  }

  private def createQuery(queryString: String, version: Version, analyzer: Analyzer): Query =
    new QueryParser(version, "title", analyzer).parse(queryString)

  private def executeQuery(query: Query, sort: Sort, directory: Directory): Unit =
    for (reader <- DirectoryReader.open(directory)) {
      println(s"========== Start ExecuteQuery[$query] ==========")

      val searcher = new IndexSearcher(reader)
      val limit = 1000
      
      val collector =
        TopFieldCollector
          .create(sort,
                  limit,
                  true,
                  false,
                  false,
                  false)

      searcher.search(query, collector)

      val topDocs = collector.topDocs
      val hits = topDocs.scoreDocs

      hits.foreach { h =>
        val hitDoc = searcher.doc(h.doc)

        println(s"Doc, id[${h.doc}]:" + System.lineSeparator +
                hitDoc
                  .getFields
                  .asScala
                  .map(f => s"${f.name}:${f.stringValue}")
                  .mkString("  ", System.lineSeparator + "  ", ""))
      }

      println(s"========== End ExecuteQuery[$query] ==========")
      println()
    }

  implicit class CloseableWrapper[A <: AutoCloseable](val underlying: A) extends AnyVal {
    def foreach(fun: A => Unit): Unit =
      try {
        fun(underlying)
      } finally {
        underlying.close()
      }
  }
}
