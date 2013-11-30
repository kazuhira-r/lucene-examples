import scala.reflect.runtime.universe

import java.io.Reader

import org.apache.lucene.analysis.{Analyzer, TokenFilter, Tokenizer, TokenStream}
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.apache.lucene.analysis.ja.JapaneseTokenizer
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter
import org.apache.lucene.analysis.ngram.EdgeNGramTokenizer
import org.apache.lucene.analysis.ngram.NGramTokenFilter
import org.apache.lucene.analysis.ngram.NGramTokenizer
import org.apache.lucene.analysis.ngram.Lucene43EdgeNGramTokenizer
import org.apache.lucene.analysis.ngram.Lucene43NGramTokenizer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.search.{BooleanQuery, Query}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.Version

object LuceneNGram {
  def main(args: Array[String]): Unit = {
    val texts = Array(
      "This is a pen.",
      "オープンソース、全文検索エンジン",
      "Apache Luceneで遊んでみます",
      "石川遼"
    )

    val analyzers = Array(
      // NGramの場合、minGramとmaxGramを指定しなければ1, 2になる
      tokenizerToAnalyzer((v, r) => new NGramTokenizer(v, r, 2, 2)),
      // EdgeNGramの場合、minGramとmaxGramを指定しない場合は、1, 1（Uni-Gram）になる
      tokenizerToAnalyzer((v, r) => new EdgeNGramTokenizer(v, r, 2, 2)),
      tokenizerToAnalyzer((v, r) => new Lucene43NGramTokenizer(r, 2, 2)),
      tokenizerToAnalyzer((v, r) => new Lucene43EdgeNGramTokenizer(v, r, 2, 2)),
      tokenizerToAnalyzer((v, r) => new NGramTokenizer(v, r, 1, 3)),
      tokenizerToAnalyzer((v, r) => new EdgeNGramTokenizer(v, r, 1, 3)),
      tokenizerToAnalyzer((v, r) => new Lucene43NGramTokenizer(r, 1, 3)),
      tokenizerToAnalyzer((v, r) => new Lucene43EdgeNGramTokenizer(v, r, 1, 3)),
      filterToAnalyzer((v, ts) => new NGramTokenFilter(v, ts, 2, 2)),
      filterToAnalyzer((v, ts) => new EdgeNGramTokenFilter(v, ts, 2, 2)),
      filterToAnalyzer((v, ts) => new NGramTokenFilter(v, ts, 1, 3)),
      filterToAnalyzer((v, ts) => new EdgeNGramTokenFilter(v, ts, 1, 3))
    )

    for {
      text <- texts
      (analyzer, c) <- analyzers
    } {
      println(s"===== Tokenizer or Filter[${c.getSimpleName}], Text[$text] START =====")

      val tokenStream = analyzer.tokenStream("", text)

      val charTermAttr = tokenStream.getAttribute(classOf[CharTermAttribute])

      tokenStream.reset()
      Iterator
        .continually(tokenStream)
        .takeWhile(_.incrementToken())
        .foreach(t => println(charTermAttr))

      println(s"===== Tokenizer or Filter[${c.getSimpleName}] Text[$text]END =====")

      /*
      val query = new QueryParser(Version.LUCENE_44, "", analyzer).parse(text)
      println(s"Query => $query, Class => ${query.getClass.getName}")

      def booleanQueryTrace(q: Query): Unit = q match {
        case bq: BooleanQuery =>
          for (clause <- bq.asInstanceOf[BooleanQuery].getClauses) {
            println(s"BooleanQuery => ${clause.getQuery.getClass.getName}, Occur => ${clause.getOccur.name}")
            booleanQueryTrace(clause.getQuery)
          }
        case _ => println(s"OtherQuery => $q")
      }

      booleanQueryTrace(query)
      */
    }
  }

  private def tokenizerToAnalyzer[T <: Tokenizer : universe.TypeTag](tokenizer: (Version, Reader) => T):
  (Analyzer, Class[T]) = {
    val mirror = universe.runtimeMirror(Thread.currentThread.getContextClassLoader)
    (new Analyzer {
      protected def createComponents(fieldName: String, reader: Reader): Analyzer.TokenStreamComponents =
      new Analyzer.TokenStreamComponents(tokenizer(Version.LUCENE_44, reader))
    }, mirror.runtimeClass(universe.typeOf[T]).asInstanceOf[Class[T]])
  }

  private def filterToAnalyzer[T <: TokenFilter : universe.TypeTag](tokenFilter: (Version, Tokenizer) => T):
  (Analyzer, Class[T]) = {
    val mirror = universe.runtimeMirror(Thread.currentThread.getContextClassLoader)
    (new Analyzer {
      protected def createComponents(fieldName: String, reader: Reader): Analyzer.TokenStreamComponents = {
        val luceneVersion = Version.LUCENE_44
        val tokenizer = new WhitespaceTokenizer(luceneVersion, reader)
        val tokenStream: TokenStream = tokenFilter(luceneVersion, tokenizer)
        new Analyzer.TokenStreamComponents(tokenizer, tokenStream)
      }
    }, mirror.runtimeClass(universe.typeOf[T]).asInstanceOf[Class[T]])
  }
}
