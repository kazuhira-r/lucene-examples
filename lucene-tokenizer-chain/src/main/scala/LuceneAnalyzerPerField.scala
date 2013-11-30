import scala.collection.JavaConverters._
import scala.collection._

import java.io.Reader

import org.apache.lucene.analysis.{Analyzer, AnalyzerWrapper, Tokenizer, TokenStream}
import org.apache.lucene.analysis.cjk._
import org.apache.lucene.analysis.core._
import org.apache.lucene.analysis.ja._
import org.apache.lucene.analysis.ngram._
import org.apache.lucene.analysis.standard._
import org.apache.lucene.analysis.tokenattributes._
import org.apache.lucene.analysis.util._
import org.apache.lucene.util.Version

object LuceneAnalyzerPerField {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_45
    val analyzer = createAnalyzer(luceneVersion)

    for {
      text <- Array("すもももももももものうち。",
                    "メガネは顔の一部です。",
                    "日本経済新聞でモバゲーの記事を読んだ。",
                    "Java, Scala, Groovy, Clojure",
                    "ＬＵＣＥＮＥ、ＳＯＬＲ、Lucene, Solr",
                    "ｱｲｳｴｵカキクケコさしすせそABCＸＹＺ123４５６",
                    "Lucene is a full-featured text search engine library written in Java.")
      fieldName <- Array("text-cjk", "text-ngram", "text-japanese-search")
    } {
      println(s">$fieldName:")
      println(s"  Original[$text]")

      val tokenStream = analyzer.tokenStream(fieldName, text)
      val charTermAttribute = tokenStream.addAttribute(classOf[CharTermAttribute])

      tokenStream.reset()

      println {
        Iterator
          .continually(tokenStream)
          .takeWhile(_.incrementToken())
          .map(t => charTermAttribute.toString)
            .mkString("  Tokenize[", " ", "]")
      }

      tokenStream.close()

      println()
    }
  }

  private def createAnalyzer(luceneVersion: Version): Analyzer = {
    //new JapaneseAnalyzer(luceneVersion)
    //new MyJapaneseAnalyzer(luceneVersion)
    //new AnalyzerPerField(luceneVersion)
    new AnalyzerWrapperPerField(luceneVersion)
  }
}


class MyJapaneseAnalyzer(luceneVersion: Version) extends JapaneseAnalyzer(luceneVersion) {
  override protected def createComponents(fieldName: String, reader: Reader): Analyzer.TokenStreamComponents = {
    println(s"fieldName = $fieldName")
    super.createComponents(fieldName, reader)
  }
}

class AnalyzerPerField(matchVersion: Version) extends Analyzer(Analyzer.PER_FIELD_REUSE_STRATEGY) {
  override protected def createComponents(fieldName: String, reader: Reader): Analyzer.TokenStreamComponents =
    fieldName match {
      case "text-cjk" =>
        val tokenizer = new StandardTokenizer(matchVersion, reader)
        var stream: TokenStream = new CJKWidthFilter(tokenizer)
        stream = new LowerCaseFilter(matchVersion, stream)
        stream = new CJKBigramFilter(stream)
        stream = new StopFilter(matchVersion, stream, CJKAnalyzer.getDefaultStopSet)
        new Analyzer.TokenStreamComponents(tokenizer, stream)
      case "text-ngram" =>
        val tokenizer = new WhitespaceTokenizer(matchVersion, reader)
        var stream: TokenStream = new CJKWidthFilter(tokenizer)
        stream = new NGramTokenFilter(matchVersion, stream, 2, 2)
        stream = new LowerCaseFilter(matchVersion, stream)
        new Analyzer.TokenStreamComponents(tokenizer, stream)
      case "text-japanese-search" =>
        val tokenizer = new JapaneseTokenizer(reader, null, true, JapaneseTokenizer.Mode.SEARCH)
        var stream: TokenStream = new JapaneseBaseFormFilter(tokenizer)
        stream = new JapanesePartOfSpeechStopFilter(matchVersion, stream, JapaneseAnalyzer.getDefaultStopTags)
        stream = new CJKWidthFilter(stream)
        stream = new StopFilter(matchVersion, stream, JapaneseAnalyzer.getDefaultStopSet)
        stream = new JapaneseKatakanaStemFilter(stream)
        stream = new LowerCaseFilter(matchVersion, stream)
        new Analyzer.TokenStreamComponents(tokenizer, stream)
  }
}

class AnalyzerWrapperPerField(matchVersion: Version) extends AnalyzerWrapper(Analyzer.PER_FIELD_REUSE_STRATEGY) {
  override def getWrappedAnalyzer(fieldName: String): Analyzer =
    fieldName match {
      case "text-cjk" => new CJKAnalyzer(matchVersion)
      case "text-ngram" =>
        new Analyzer {
          override protected def createComponents(fieldName: String, reader: Reader): Analyzer.TokenStreamComponents = {
            val tokenizer = new WhitespaceTokenizer(matchVersion, reader)
            var stream: TokenStream = new CJKWidthFilter(tokenizer)
            stream = new NGramTokenFilter(matchVersion, stream, 2, 2)
            stream = new LowerCaseFilter(matchVersion, stream)
            new Analyzer.TokenStreamComponents(tokenizer, stream)
          }
        }
      case "text-japanese-search" => new JapaneseAnalyzer(matchVersion)
    }
}



/*
         new TokenizerChain(
          null,
          new JapaneseTokenizerFactory(mutable.Map("luceneMatchVersion" -> matchVersion.toString,
                                                   "mode" -> "SEARCH").asJava),
          Array[TokenFilterFactory](
            new JapaneseBaseFormFilterFactory(mutable.Map("luceneMatchVersion" -> matchVersion.toString).asJava),
            new JapanesePartOfSpeechStopFilterFactory(mutable.Map("luceneMatchVersion" -> matchVersion.toString,
                                                                  "tags" -> "stopTags.txt").asJava),
            new CJKWidthFilterFactory(mutable.Map.empty[String, String].asJava),
            new StopFilterFactory(mutable.Map("luceneMatchVersion" -> matchVersion.toString,
                                              "words" -> "stopwords.txt").asJava),
            new JapaneseKatakanaStemFilterFactory(mutable.Map.empty[String, String].asJava),
            new LowerCaseFilterFactory(mutable.Map("luceneMatchVersion" -> matchVersion.toString).asJava)
          )
        )
*/

  /*
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_45
    val analyzer = createAnalyzer(luceneVersion)

    for (directory <- new RAMDirectory) {
      registerDocuments(directory, luceneVersion, analyzer)

      queryIndex(directory, luceneVersion, analyzer)
    }
  }

  private def createAnalyzer(luceneVersion: Version): Analyzer = {
    new JapaneseAnalyzer(luceneVersion)
    //new MyAnalyzer(luceneVersion)
  }

  private def registerDocuments(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (indexWriter <- new IndexWriter(directory,
                                        new IndexWriterConfig(luceneVersion, analyzer))) {
      indexWriter.addDocument(createDocument("1",
                                             "Apache Lucene 入門 ～Java・オープンソース・全文検索システムの構築"))
      indexWriter.addDocument(createDocument("2",
                                             "Apache Solr入門 ―オープンソース全文検索エンジン"))
      indexWriter.addDocument(createDocument("3",
                                             "Scalaスケーラブルプログラミング第2版"))
      indexWriter.addDocument(createDocument("4",
                                             "すもももももももものうち。"))
      indexWriter.addDocument(createDocument("5",
                                             "メガネは顔の一部です。"))
      indexWriter.addDocument(createDocument("6",
                                             "日本経済新聞でモバゲーの記事を読んだ。"))
      indexWriter.addDocument(createDocument("7",
                                             "Java, Scala, Groovy, Clojure"))
      indexWriter.addDocument(createDocument("8",
                                             "ＬＵＣＥＮＥ、ＳＯＬＲ、Lucene, Solr"))
      indexWriter.addDocument(createDocument("9",
                                             "ｱｲｳｴｵカキクケコさしすせそABCＸＹＺ123４５６"))
      indexWriter.addDocument(createDocument("10",
                                             "Lucene is a full-featured text search engine library written in Java."))
    }

  private def createDocument(id: String, text: String): Document = {
    val document = new Document
    document.add(new StringField("id", id, Field.Store.YES))
    document.add(new TextField("text-1", text, Field.Store.YES))
    document.add(new TextField("text-2", text, Field.Store.YES))
    document
  }

  private def queryIndex(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (reader <- DirectoryReader.open(directory)) {
      val searcher = new IndexSearcher(reader)
      val queryParser = new QueryParser(luceneVersion, "text-1", analyzer)

      val limit = 1000

      val queries = Array("Scala", "Scala id:Lucene")

      queries.foreach { queryString =>
        val query = queryParser.parse(queryString)

        val totalHitCountCollector = new TotalHitCountCollector
        searcher.search(query, totalHitCountCollector)

        val totalHits = totalHitCountCollector.getTotalHits

        val docCollector = TopFieldCollector.create(Sort.RELEVANCE,
                                                    limit,
                                                    true,
                                                    false,
                                                    false,
                                                    false)

        searcher.search(query, docCollector)

        println(s"Query => $query")
        println(s"${totalHits}件ヒットしました")

        docCollector.topDocs.scoreDocs.foreach { h =>
          val hitDoc = searcher.doc(h.doc)
          println(s"Score,ID[${h.score}:${h.doc}]: Doc => " +
                  hitDoc
                    .getFields
                    .asScala
                    .map(_.stringValue)
                    .mkString(" | "))
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
  */

