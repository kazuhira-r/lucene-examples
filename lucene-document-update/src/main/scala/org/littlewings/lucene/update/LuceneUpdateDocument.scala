package org.littlewings.lucene.update

import scala.collection.JavaConverters._

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexableField, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery, Sort, SortField, TermQuery, TopFieldCollector}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

object LuceneUpdateDocument {
  def main(args: Array[String]): Unit = {
    val version = Version.LUCENE_CURRENT
    val analyzer = createAnalyzer(version)

    for (directory <- new RAMDirectory) {
      registerDocuments(directory, version, analyzer)

      printAllDocuments(directory, version)

      println("==================================================")

      updateDocument(directory,
                     version,
                     analyzer,
                     new Term("isbn", "978-4774127804"),
                     new StringField("price", "5000", Field.Store.YES))

      updateDocument(directory,
                     version,
                     analyzer,
                     new Term("isbn", "978-4797352009"),
                     new TextField("title", "【集合知イン・アクション】", Field.Store.YES),
                     new StringField("price", "2000", Field.Store.YES))

      printAllDocuments(directory, version)
    }
  }

  private def createAnalyzer(version: Version): Analyzer =
    new JapaneseAnalyzer(version)

  private def registerDocuments(directory: Directory, version: Version, analyzer: Analyzer): Unit =
    for (writer <- new IndexWriter(directory,
                                   new IndexWriterConfig(version, analyzer))) {
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
      )
      .foreach(writer.addDocument)

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

  private def printAllDocuments(directory: Directory, version: Version): Unit =
    for (reader <- DirectoryReader.open(directory)) {
      val searcher = new IndexSearcher(reader)
      val allQuery = new MatchAllDocsQuery
      val limit = 1000

      val collector =
        TopFieldCollector
          .create(new Sort(new SortField("price", SortField.Type.INT, true)),
                  limit,
                  true,
                  false,
                  false,
                  false)

      searcher.search(allQuery, collector)

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
    }

  private def updateDocument(directory: Directory,
                             version: Version,
                             analyzer: Analyzer,
                             term: Term,
                             fields: IndexableField*): Unit =
    for {
      writer <- new IndexWriter(directory,
                                new IndexWriterConfig(version, analyzer))
      reader <- DirectoryReader.open(directory)
    } {
      val searcher = new IndexSearcher(reader)
      val query = new TermQuery(term)

      val hits = searcher.search(query, null, 1).scoreDocs
      
      if (hits.size > 0) {
        val hitDoc = searcher.doc(hits(0).doc)

        fields.foreach { f =>
          hitDoc.removeField(f.name)
          hitDoc.add(f)
        }

        writer.updateDocument(term, hitDoc)

        writer.commit()
      }
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
