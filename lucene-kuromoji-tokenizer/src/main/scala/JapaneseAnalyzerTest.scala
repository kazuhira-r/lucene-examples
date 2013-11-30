import java.io.{Reader, StringReader}

import org.apache.lucene.analysis.{Analyzer, Tokenizer, TokenStream}
import org.apache.lucene.analysis.cjk.CJKWidthFilter
import org.apache.lucene.analysis.core.{LowerCaseFilter, StopFilter}
import org.apache.lucene.analysis.ja._
import org.apache.lucene.analysis.ja.dict.UserDictionary
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.util.Version

object JapaneseAnalyzerTest {
  def main(args: Array[String]): Unit = {
    val texts =
      List(
        "すもももももももものうち。",
        "メガネは顔の一部です。",
        "日本経済新聞でモバゲーの記事を読んだ。",
        "Java, Scala, Groovy, Clojure",
        "ＬＵＣＥＮＥ、ＳＯＬＲ、Lucene, Solr",
        "ｱｲｳｴｵカキクケコさしすせそABCＸＹＺ123４５６",
        "Lucene is a full-featured text search engine library written in Java."
      )

    val modes =
      List(
        JapaneseTokenizer.Mode.SEARCH,
        JapaneseTokenizer.Mode.NORMAL,
        JapaneseTokenizer.Mode.EXTENDED
      )

    for {
      text <- texts
      mode <- modes
    } withJapaneseAnalyzer(text, mode)(displayTokens)
  }

  def withJapaneseAnalyzer(text: String, mode: JapaneseTokenizer.Mode)(body: (String, TokenStream) => Unit): Unit = {
    val japaneseAnalyzer = createJapaneseAnalyzer(mode)
    val tokenStream = japaneseAnalyzer.tokenStream("", new StringReader(text))

    println(s"Mode => $mode Start")

    try {
      body(text, tokenStream)
    } finally {
      tokenStream.close()
    }

    println(s"Mode => $mode End")
    println()
  }

  def createJapaneseAnalyzer(mode: JapaneseTokenizer.Mode): JapaneseAnalyzer = {
    val userDict: UserDictionary = null
    val stopwords = JapaneseAnalyzer.getDefaultStopSet
    val stoptags = JapaneseAnalyzer.getDefaultStopTags

    new JapaneseAnalyzer(Version.LUCENE_43,
                         null,
                         mode,
                         stopwords,
                         stoptags) {
      override def createComponents(fieldName: String, reader: Reader): Analyzer.TokenStreamComponents = {
        val tokenizer = new JapaneseTokenizer(reader, userDict, true, mode)
        var stream: TokenStream = new JapaneseBaseFormFilter(tokenizer)
        stream = new JapanesePartOfSpeechStopFilter(true, stream, stoptags)
        stream = new CJKWidthFilter(stream)
        stream = new StopFilter(matchVersion, stream, stopwords)
        stream = new JapaneseKatakanaStemFilter(stream)
        stream = new LowerCaseFilter(matchVersion, stream)
        new Analyzer.TokenStreamComponents(tokenizer, stream)
      }
    }
  }

  def displayTokens(text: String, tokenStream: TokenStream): Unit = {
    val charTermAttr = tokenStream.addAttribute(classOf[CharTermAttribute])

    println("<<==========================================")
    println(s"input text => $text")
    println("============================================")

    tokenStream.reset()

    while (tokenStream.incrementToken()) {
      val token = charTermAttr.toString
      println(s"token: $token")
    }

    tokenStream.close()
  }
}
