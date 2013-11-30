import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query, Sort, SortField, TopDocs}
import org.apache.lucene.search.{TopFieldCollector, TotalHitCountCollector, TopScoreDocCollector}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

import org.supercsv.io.CsvListReader
import org.supercsv.prefs.CsvPreference

import LuceneCollector.AutoCloseableWrapper

object LuceneCollector {
  def main(args: Array[String]): Unit = {
    val directory = new RAMDirectory
    val luceneVersion = Version.LUCENE_43
    
    val indexer = new Indexer(directory,
                              luceneVersion,
                              new JapaneseAnalyzer(luceneVersion),
                              "zenkoku.csv")
    indexer.execute()

    InteractiveQuery.queryWhile(directory, luceneVersion, new JapaneseAnalyzer(luceneVersion))
  }

  implicit class AutoCloseableWrapper[A <: AutoCloseable](val underlying: A) extends AnyVal {
    def foreach(f: A => Unit): Unit =
      try {
        f(underlying)
      } finally {
        if (underlying != null) {
          underlying.close()
        }
      }
  }
}

class Indexer(directory: Directory, luceneVersion: Version, analyzer: Analyzer, source: String) {
  def execute(): Unit = {
    for {
      reader <- Files.newBufferedReader(Paths.get(source), Charset.forName("Windows-31J"))
      csvReader <- new CsvListReader(reader, CsvPreference.STANDARD_PREFERENCE)
      indexWriter <- new IndexWriter(directory,
                                     new IndexWriterConfig(luceneVersion,
                                                           analyzer))
    } {
      val count =
        Iterator
          .continually(csvReader.read())
          .takeWhile(_ != null)
          .foldLeft(0) { (acc, tokens) =>
            if (acc > 0 && acc % 10000 == 0) {
              printf("%1$,3d件…%n", acc)
            }

            indexWriter.addDocument(Address(tokens.asScala).toDocument)

            acc + 1
          }

      printf("%1$,3d件、インデックスに登録しました%n", count)
    }
  }
}

case class Address(tokens: Seq[String]) {
  def toDocument: Document = {
    val doc = new Document
    doc.add(stringField("addressCd", 0))
    doc.add(stringField("zipNo", 4))
    doc.add(textField("prefecture", 7))
    doc.add(textField("prefectureKana", 8))
    doc.add(textField("city", 9))
    doc.add(textField("cityKana", 10))
    doc.add(textField("town", 11))
    doc.add(textField("townKana", 12))
    doc.add(textField("azachome", 15))
    doc.add(textField("azachomeKana", 16))
    doc
  }
  private def stringField(name: String, index: Int): Field =
    new StringField(name, Option(tokens(index)).getOrElse(""), Field.Store.YES)

  private def textField(name: String, index: Int): Field =
    new TextField(name, Option(tokens(index)).getOrElse(""), Field.Store.YES)
}

object InteractiveQuery {
  private var currentPage: Int = 1
  private var offset: Int = 20
  private val max: Int = 2000000

  def queryWhile(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit = {
    println("Start Interactive Query")

    for (reader <- DirectoryReader.open(directory)) {
      val searcher = new IndexSearcher(reader)

      val qp: QueryParser = new QueryParser(luceneVersion, "prefecture", analyzer)
      // qp.setDefaultOperator(QueryParser.Operator.AND)

      val query = (queryString: String) =>
        Try(qp.parse(queryString))
          .recoverWith { case th =>
                         println(s"[ERROR] Invalid Query: $th")
                         Failure(th)
                       }.toOption

      val pageRegex = """page\s*=\s*(\d+)""".r
      val changePaging: PartialFunction[String, Unit] = {
        case pageRegex(page) =>
          currentPage = Try(page.toInt).recover { case _ => 0 }.get
          println(s"set currentPage = $currentPage")
      }

      val offsetRegex = """offset\s*=\s*(\d+)""".r
      val changeOffset: PartialFunction[String, Unit] = {
        case offsetRegex(o) =>
          offset = Try(o.toInt).recover { case _ => 20 }.get
          println(s"set offset = $offset")
      }

      val executeQuery: PartialFunction[String, Unit] = {
        case line =>
          query(line).foreach { q =>
            println(s"入力したクエリ => $q")

            // ヒット件数の取得
            val totalHitCountCollector = new TotalHitCountCollector

            searcher.search(q, totalHitCountCollector)

            val totalHits = totalHitCountCollector.getTotalHits

            printf("%1$,3d件、ヒットしました%n", totalHits)

            val start = (currentPage - 1) * offset
            if (totalHits > start) {
              // 通常の、スコア順での検索
              // val docCollector =
                // TopScoreDocCollector.create(currentPage * offset, true)

              // ソート有りの検索
              val docCollector =
                TopFieldCollector.create(new Sort(new SortField("addressCd",
                                                                   SortField.Type.STRING,
                                                                   true),
                                                     new SortField("zipNo",
                                                                   SortField.Type.STRING,
                                                                   true)),
                                            currentPage * offset,
                                            true,  // fillFields
                                            false,  // trackDocScores
                                            false,  // trackMaxScore
                                            false)  // docScoredInOrder
              // TopFieldCollector.createのafterがある版も確認

              searcher.search(q, docCollector)

              val end =
                if ((currentPage * offset) > totalHits) totalHits
                else currentPage * offset

              // val topDocs = docCollector.topDocs  // ここ、追加！！
              val topDocs = docCollector.topDocs(start, currentPage * offset)
              val hits = topDocs.scoreDocs

              printf("ヒットレコード中の、%1$,3d～%2$,3d件までを表示します%n", start + 1, end)

              hits foreach { h =>
                val hitDoc = searcher.doc(h.doc)
                println { s"Score,N[${h.score}:${h.doc}] : Doc => " +
                          hitDoc
                            .getFields
                            .asScala
                            .map(_.stringValue)
                            .mkString("  ", " | ", "")
                        }
              }

              /*
              (start until end).foreach { i =>
                val h = hits(i)
                val hitDoc = searcher.doc(h.doc)
                println { s"Score,N[${h.score}:${h.doc}] : Doc => " +
                          hitDoc
                            .getFields
                            .asScala
                            .map(_.stringValue)
                            .mkString("  ", " | ", "")
                        }
              }
              */
            } else {
              printf("開始位置が%1$,3dのため、表示するレコードがありません%n", start + 1)
            }
          }
        }

      Iterator
        .continually(readLine("Lucene Query> "))
        .takeWhile(_ != "exit")
        .withFilter(line => !line.isEmpty && !line.endsWith("\\c"))
        .foreach(changePaging orElse changeOffset orElse executeQuery) 
    }
  }
}
