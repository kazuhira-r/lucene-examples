package org.littlewings.lucene.fuzzymorelikethis

import scala.collection.JavaConverters._

import java.io.StringReader

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.queries.mlt.{MoreLikeThis, MoreLikeThisQuery}
import org.apache.lucene.search.{BooleanQuery, FuzzyQuery, IndexSearcher, MatchAllDocsQuery, Query, Sort, TermQuery}
import org.apache.lucene.search.{ScoreDoc, TopDocs, TopFieldCollector, TotalHitCountCollector}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

object LuceneFuzzyMoreLikeThis {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_CURRENT
    def analyzer = createAnalyzer(luceneVersion)

    for (directory <- new RAMDirectory) {
      registerDocuments(directory, luceneVersion, analyzer)

      matchAllDocsQueries(directory)

      termQueries(directory,
                  analyzer,
                  new Term("text", "java"),
                  new Term("text", "jabo"),
                  new Term("text", "日本"),
                  new Term("text", "日韓"),
                  new Term("text", "メガホン"))

      fuzzyQueries(directory,
                   analyzer,
                   (new Term("text", "java"), 2),
                   (new Term("text", "jabo"), 1),
                   (new Term("text", "jabo"), 2),
                   (new Term("text", "日本"), 2),
                   (new Term("text", "日韓"), 2),
                   (new Term("text", "メガホン"), 2))

      moreLikeThisQueries(directory,
                          analyzer,
                          3,
                          6)
    }
  }

  private def createAnalyzer(version: Version): Analyzer =
    new JapaneseAnalyzer(version)

  private def registerDocuments(directory: Directory, version: Version, analyzer: Analyzer): Unit =
    for {
      writer <- new IndexWriter(directory,
                                new IndexWriterConfig(version, analyzer))
      text <- Array("すもももももももものうち。",
                    "メガネは顔の一部です。",
                    "日本経済新聞でモバゲーの記事を読んだ。",
                    "Java, Scala, Groovy, Clojure",
                    "ＬＵＣＥＮＥ、ＳＯＬＲ、Lucene, Solr",
                    "ｱｲｳｴｵカキクケコさしすせそABCＸＹＺ123４５６",
                    "Lucene is a full-featured text search engine library written in Java.")
    } {
      val document = new Document
      document.add(new TextField("text", text, Field.Store.YES))
      writer.addDocument(document)
    }

  private def matchAllDocsQueries(directory: Directory): Unit = {
    println("==================== MatchAllDocsQuery Start ====================")

    for (reader <- DirectoryReader.open(directory)) {
      val query = new MatchAllDocsQuery
      searchAndExplain(reader, query)
    }

    println("==================== MatchAllDocsQuery End ====================")
  }

  private def termQueries(directory: Directory,
                           analyzer: Analyzer,
                           terms: Term*): Unit = {
    println("==================== TermQuery Start ====================")

    for {
      reader <- DirectoryReader.open(directory)
      term <- terms
    } {
      val query = new TermQuery(term)
      searchAndExplain(reader, query)
    }

    println("==================== TermQuery End ====================")
  }

  private def fuzzyQueries(directory: Directory,
                           analyzer: Analyzer,
                           terms: (Term, Int)*): Unit = {
    println("==================== FuzzyQuery Start ====================")

    for {
      reader <- DirectoryReader.open(directory)
      (term, maxEdit) <- terms
    } {
      val query = new FuzzyQuery(term, maxEdit)

      println("Rewrited Query And Term => "
              + query
              .rewrite(reader)
              .asInstanceOf[BooleanQuery]
              .getClauses
              .flatMap { bq =>
                Array(bq, bq.getQuery.asInstanceOf[TermQuery].getTerm.text)
              }
              .mkString(", "))

      searchAndExplain(reader, query)
    }

    println("==================== FuzzyQuery End ====================")
  }

  private def moreLikeThisQueries(directory: Directory,
                                  analyzer: Analyzer,
                                  docIds: Int*): Unit = {
    println("==================== MoreLikeThisQuery Start ====================")

    for {
      reader <- DirectoryReader.open(directory)
      docId <- docIds
    } {
      val mlt = new MoreLikeThis(reader)
      mlt.setAnalyzer(analyzer)
      mlt.setFieldNames(Array("text"))
      mlt.setMinTermFreq(0)
      mlt.setMinDocFreq(0)

      // すでに検索を行ったつもりで、DocumentのIDを受け取ることとする
      val query = mlt.like(docId)

      searchAndExplain(reader, query)
    }

    println("==================== MoreLikeThisQuery End ====================")
  }

  private def searchAndExplain(reader: IndexReader,
                               query: Query): Unit = {
    val searcher = new IndexSearcher(reader)

    println(s"Input Query => [$query]")

    val totalHitCountCollector = new TotalHitCountCollector
    searcher.search(query, totalHitCountCollector)
    val totalHits = totalHitCountCollector.getTotalHits

    val docCollector =
      TopFieldCollector.create(Sort.RELEVANCE,
                               1000,
                               true,
                               true,
                               true,
                               true)

    searcher.search(query, docCollector)
    val topDocs = docCollector.topDocs
    val hits = topDocs.scoreDocs

    hits.foreach { h =>
      println("---------------")
      val hitDoc = searcher.doc(h.doc)
      println(s"   ScoreDoc, id[${h.score}:${h.doc}]: Doc => " +
              hitDoc
                .getFields
                .asScala
                .map(_.stringValue)
                .mkString("|"))

      val explanation = searcher.explain(query, h.doc)

      println()
      println("Explanation As String => ")
      explanation.toString.lines.map("    " + _).foreach(println)
      println("---------------")
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
