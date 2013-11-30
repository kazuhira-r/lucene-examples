import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, TextField, StringField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, Sort, SortField}
import org.apache.lucene.search.{ScoreDoc, TopFieldCollector, TotalHitCountCollector}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

import org.apache.lucene.search.highlight.{Highlighter, QueryScorer, SimpleHTMLFormatter, TokenSources}

object LuceneHighlighter {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_44
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    for (directory <- new RAMDirectory) {
      registryDocuments(directory, luceneVersion, analyzer)

      queryWhile(directory, luceneVersion, analyzer)
    }
  }

  private def registryDocuments(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (indexWriter <- new IndexWriter(directory,
                                        new IndexWriterConfig(luceneVersion, analyzer))) {
      indexWriter.addDocument(createBook("978-4894714991",
                                         "Effective Java 第2版",
                                         3780,
                                         "java",
                                         "Javaプログラミング書籍の定本「Effective Java」の改訂版です。著者のGoogle, Sun Microsystemsにおけるソフトウェア開発で得た知識・経験をまとめた、JavaでプログラミングをするすべてのSE必読の書籍です。2001年の初版以降の追加項目、JavaSE6.0に対応。"))
      indexWriter.addDocument(createBook("978-4774139906",
                                         "パーフェクトJava",
                                         3780,
                                         "java",
                                         "本書はJavaで開発を行う人へのバイブル的1冊です。Javaの基本から説明していますが、プログラミング一般の考え方や技法まで解説しています。"))
      indexWriter.addDocument(createBook("978-4774158785",
                                         "AndroidエンジニアのためのモダンJava",
                                         3360,
                                         "java",
                                         "本書は、複雑かつ高度なAndroidアプリケーションの開発に必要となる、Java言語の基礎を理解することに主眼を置いて執筆されています。"))
      indexWriter.addDocument(createBook("978-4844330844",
                                         "Scalaスケーラブルプログラミング第2版",
                                         4830,
                                         "scala",
                                         "言語設計者自ら、その手法と思想を説くScalaプログラミングバイブル!"))
      indexWriter.addDocument(createBook("978-4798125411",
                                         "Scala逆引きレシピ (PROGRAMMER’S RECiPE)",
                                         3360,
                                         "scala",
                                         "Scalaでコードを書く際の実践ノウハウが凝縮!"))
      indexWriter.addDocument(createBook("978-4774127804",
                                         "Apache Lucene 入門 ～Java・オープンソース・全文検索システムの構築",
                                         3360,
                                         "lucene",
                                         "Luceneは全文検索システムを構築するためのJavaのライブラリです。"))
      indexWriter.addDocument(createBook("978-4774141756",
                                         "Apache Solr入門 ―オープンソース全文検索エンジン",
                                         3780,
                                         "solr",
                                         "Apache Solrとは,オープンソースの検索エンジンです.Apache LuceneというJavaの全文検索システムをベースに豊富な拡張性をもたせ,多くの開発者が利用できるように作られました."))
    }

  private def createBook(isbn13: String,
                         title: String,
                         price: Int,
                         category: String,
                         abstraction: String): Document = {
    val document = new Document
    document.add(new StringField("isbn13", isbn13, Field.Store.YES))
    document.add(new TextField("title", title, Field.Store.YES))
    document.add(new StringField("price", price.toString, Field.Store.YES))
    document.add(new StringField("category", category, Field.Store.YES))
    document.add(new TextField("abstraction", abstraction, Field.Store.YES))
    document
  }

  private def queryWhile(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (reader <- DirectoryReader.open(directory)) {
      val searcher = new IndexSearcher(reader)
      val queryParser = new QueryParser(luceneVersion, "title", analyzer)
      val limit = 1000

      def parseQuery(line: String): Try[Query] =
        Try(queryParser.parse(line))

      def search(query: Query): (Query, Int, Array[ScoreDoc]) = {
        println(s"Query => [$query]")

        val totalHitCountCollector = new TotalHitCountCollector
        searcher.search(query, totalHitCountCollector)

        val totalHits = totalHitCountCollector.getTotalHits

        val docCollector = TopFieldCollector.create(new Sort(new SortField("price",
                                                                           SortField.Type.STRING)),
                                                    limit,
                                                    true,
                                                    false,
                                                    false,
                                                    false)

        searcher.search(query, docCollector)
        
        (query, totalHits, docCollector.topDocs.scoreDocs)
      }

      Iterator
        .continually(readLine("Lucene Query> "))
        .withFilter(l => l != null && !l.isEmpty)
        .takeWhile(l => l != "exit")
        .map(parseQuery)
        .withFilter(q => q.recoverWith {
          case e =>
            println(s"Invalid Query => $e")
            Failure(e)
        }.isSuccess)
        .map(s => search(s.get))
        .foreach { case (query, n, hits) =>
          if (n > 0) {
            println(s"$n 件ヒットしました")

            val fields = Array("title", "abstraction")
            val htmlFormatter = new SimpleHTMLFormatter()  // 何もしていないと、<b></b>で囲まれる
            // val htmlFormatter = new SimpleHTMLFormatter("<bold>", "</bold>")  // コンストラクタで指定可能
            val highlighter = new Highlighter(htmlFormatter, new QueryScorer(query))
            val mergeContiguousFragments = false
            val maxNumFragments = 10

            for (h <- hits) {
              val hitDoc = searcher.doc(h.doc)

              println(s"Score,ID[${h.score}:${h.doc}] : Doc => " +
                      hitDoc
                        .getFields
                        .asScala
                        .map(_.stringValue)
                        .mkString(" ", " | ", ""))

              // Highlighter
              fields.foreach { field =>
                val text = hitDoc.get(field)  // ハイライト対象のフィールドをDocumentから取得
                val tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader,
                                                                 h.doc,
                                                                 field,
                                                                 analyzer)
                val fragments = highlighter.getBestTextFragments(tokenStream,
                                                                 text,
                                                                 mergeContiguousFragments,
                                                                 maxNumFragments)

                fragments
                  .withFilter(_.getScore > 0)
                  .foreach(f => println(s"    Fragment => $f"))
              }
            }
          } else {
            println("お探しの本はありませんでした")
          }
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
