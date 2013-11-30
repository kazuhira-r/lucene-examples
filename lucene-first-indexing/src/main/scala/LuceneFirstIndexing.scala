import scala.util.{Failure, Success, Try}

import java.io.File
import java.text.SimpleDateFormat

import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.CompressionTools
import org.apache.lucene.document.Document
import org.apache.lucene.document.DateTools
import org.apache.lucene.document.Field
import org.apache.lucene.document.IntField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version

object LuceneIndexCreater {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_43
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    val directory = FSDirectory.open(new File("index-dir"))
    try {
      val config = new IndexWriterConfig(luceneVersion, analyzer)
      val writer = new IndexWriter(directory, config)

      try {
        writer.deleteAll()

        createDocs.foreach(writer.addDocument)
      } finally {
        writer.close()
      }
    } finally {
      directory.close()
    }
  }

  def createDocs: List[Document] =
    List(
      createBookDoc("Apache Lucene 入門 Java・オープンソース・全文検索システムの構築",
                    3200,
                    "2006/05/17",
                    "Luceneは全文検索システムを構築するためのJavaのライブラリです。Luceneを使えば,一味違う高機能なWebアプリケーションを作ることができます。"),
      createBookDoc("Apache Solr入門 オープンソース全文検索エンジン",
                    3780,
                    "2010/02/20",
                    "Apache Solrとは,オープンソースの検索エンジンです.Apache LuceneというJavaの全文検索システムをベースに豊富な拡張性をもたせ,多くの開発者が利用できるように作られました.検索というと,Googleのシステムを使っている企業Webページが多いですが,自前の検索エンジンがあると顧客にとって本当に必要な情報を提供できます"),
      createBookDoc("JBoss徹底活用ガイド",
                    2919,
                    "2008/02/19",
                    "企業向けJava+Web開発の決定版」企業向けのJavaの定番オープンソース製品になったJBoss。本書は JBoss ユーザ会の有志による共同執筆。現場で培われたJBossの活用ノウハウを余すことなく本書に反映。"),
      createBookDoc("Seasar 2 徹底入門 SAStruts/S2JDBC 対応",
                    3990,
                    "2010/04/20",
                    "Seasar2を使いこなすためのバイブルが登場!!")
    )

  def createBookDoc(title: String, price: Int, publishDate: String, description: String): Document = {
    val doc = new Document
    doc.add(new TextField("title",
                          title,
                          Field.Store.YES))
    doc.add(new StringField("price",
                            price.toString,
                            Field.Store.YES))
    doc.add(new StringField("publish-date",
                            DateTools.dateToString(new SimpleDateFormat("yyyy/MM/dd").parse(publishDate),
                                                   DateTools.Resolution.DAY),
                            Field.Store.YES))
    doc.add(new TextField("description",
                          description,
                          Field.Store.YES))
    doc
  }
}

object LuceneIndexSearcher {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_43
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    val directory = FSDirectory.open(new File("index-dir"))
    try {
      val reader = DirectoryReader.open(directory)
      val searcher = new IndexSearcher(reader)

      Iterator
        .continually(readLine("""Enter Search Query (if "exit" type, exit)> """))
        .takeWhile(_ != "exit")
        .filter(_ != "")
        .foreach { line =>
          val parser = new QueryParser(luceneVersion, "title", analyzer)
          Try {
            val query = parser.parse(line)

            println(s"Query => $query")

            val hits = searcher.search(query, null, 1000).scoreDocs

            println(s"hits => ${hits.length}")

            for (h <- hits) {
              val hitDoc = searcher.doc(h.doc)
              println(s"Hit Document => $hitDoc")
            }
          } match {
            case Success(_) =>
            case Failure(e) => println(e)
          }
        }

      reader.close()
    } finally {
      directory.close()
    }
  }
}

object LuceneIndexDeleter {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_43
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    val directory = FSDirectory.open(new File("index-dir"))
    try {
      val config = new IndexWriterConfig(luceneVersion, analyzer)
      val writer = new IndexWriter(directory, config)

      try {
        Iterator
          .continually(readLine("""Enter Delete Query (if "exit" type, exit)> """))
          .takeWhile(_ != "exit")
          .filter(_ != "")
          .foreach { line =>
            val parser = new QueryParser(luceneVersion, "title", analyzer)

            Try {
              val query = parser.parse(line)

              println(s"Query => $query")

              writer.deleteDocuments(query)

              println("Document Deleted")
            } match {
              case Success(_) =>
              case Failure(e) => println(e)
            }
        }
      } finally {
        writer.close()
      }
    } finally {
      directory.close()
    }
  }
}

