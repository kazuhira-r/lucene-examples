import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, Query}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

import au.com.bytecode.opencsv.CSVReader

import LuceneQueryParserExample.CloseableWrapper

object LuceneQueryParserExample {
  def main(args: Array[String]): Unit = {
    val indexer = new AddressIndexer("zenkoku.csv")
    indexer.execute()

    println()

    InteractiveQuery.queryWhile()
  }

  implicit class CloseableWrapper[A <: AutoCloseable](val underlying: A) extends AnyVal {
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

object DirectoryRepository {
  val directory: Directory = new RAMDirectory

  def withDir[A](body: (Directory, Version) => A): A = {
    val version = Version.LUCENE_43
    body(directory, version)
  }
}

class AddressIndexer(fileName: String) {
  def execute(): Unit = {
    println(s"インデックスへの登録を開始します。入力ファイル => $fileName...")

    for (reader <- new CSVReader(Files.newBufferedReader(Paths.get(fileName),
                                                         Charset.forName("Windows-31J")),
                                 ',',
                                 '"')) {
      DirectoryRepository.withDir { (directory, version) =>
        val config = new IndexWriterConfig(version, new JapaneseAnalyzer(version))
        for (writer <- new IndexWriter(directory, config)) {
          val count =
            Iterator
              .continually(reader.readNext)
              .takeWhile(_ != null)
              .foldLeft(0) { (acc, tokens) =>
                if (acc > 0 && acc % 10000 == 0) {
                  printf("%1$,3d件…%n", acc)
                }

                writer.addDocument(new Address(tokens).toDocument)
                acc + 1
            }

          printf("%1$,3d件、インデックスに登録しました%n", count)
        }
      }
    }
  }
}

class Address(tokens: Array[String]) {
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
    new StringField(name, tokens(index), Field.Store.YES)

  private def textField(name: String, index: Int): Field =
    new TextField(name, tokens(index), Field.Store.YES)
}

object InteractiveQuery {
  def queryWhile(): Unit = {
    println("Start Interactive Query")

    DirectoryRepository.withDir { (directory, version) =>
      def query(queryString: String): Option[Query] =
        Try {
          val qp = new QueryParser(version, "prefecture", new JapaneseAnalyzer(version))
          qp.setDefaultOperator(QueryParser.Operator.AND)
          qp.parse(queryString)
        } match {
          case Success(q) => Some(q)
          case Failure(th) =>
            println(s"[ERROR] Invalid Query: $th")
            None
        }

      for (reader <- DirectoryReader.open(directory)) {
        val searcher = new IndexSearcher(reader)

        Iterator
          .continually(readLine("Lucene Query> "))
          .takeWhile(_ != "exit")
          .withFilter(l => l != "" && !l.endsWith("\\c"))
          .foreach { line =>
            query(line).foreach { q =>
              println(s"入力したクエリ => $q")

              val hits = searcher.search(q, null, 200000).scoreDocs
              
              printf("%1$,3d件、ヒットしました%n", hits.length)

              if (hits.length > 0) {
                val n = 10
                printf("最初の%1$,3d件を表示します%n", n)

                hits.take(n).foreach { h =>
                  val hitDoc = searcher.doc(h.doc)
                  println { s"Score,N[${h.score}:${h.doc}] : Doc => " +
                            hitDoc
                              .getFields
                              .asScala
                              .map(_.stringValue)
                              .mkString("  ", " | ", "")
                          }
                }
              }
            }
          }
      }
    }

    println("Exit Interactive Query")
  }
}
