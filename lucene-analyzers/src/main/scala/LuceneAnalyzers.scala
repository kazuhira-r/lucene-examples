import java.io.StringReader

import org.apache.lucene.analysis.{Analyzer, TokenStream}
import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.ja.{JapaneseAnalyzer, JapaneseTokenizer}
import org.apache.lucene.analysis.ja.tokenattributes.{BaseFormAttribute, PartOfSpeechAttribute, ReadingAttribute, InflectionAttribute}
import org.apache.lucene.analysis.ja.dict.UserDictionary
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, OffsetAttribute, PositionIncrementAttribute, TypeAttribute}
import org.apache.lucene.util.Version

object LuceneAnalyzers {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_43

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

    usingTokenStream(new StandardAnalyzer(luceneVersion), texts: _*)(displayTokens)

    usingTokenStream(new WhitespaceAnalyzer(luceneVersion), texts: _*)(displayTokens)

    usingTokenStream(new KeywordAnalyzer, texts: _*)(displayTokens)

    usingTokenStream(new CJKAnalyzer(luceneVersion), texts: _*)(displayTokens)

    val userDictionary: UserDictionary = null
    //val mode = JapaneseTokenizer.Mode.SEARCH
    //val mode = JapaneseTokenizer.Mode.NORMAL
    val mode = JapaneseTokenizer.Mode.EXTENDED
    val stopwords = JapaneseAnalyzer.getDefaultStopSet
    val stoptags = JapaneseAnalyzer.getDefaultStopTags

    usingTokenStream(new JapaneseAnalyzer(luceneVersion,
                                          userDictionary,
                                          mode,
                                          stopwords,
                                          stoptags),
                     texts: _*)(displayTokens)

    //usingTokenStream(new JapaneseAnalyzer(luceneVersion), texts: _*)(displayTokens)
  }

  def usingTokenStream(analyzer: Analyzer, texts: String*)(body: (String, TokenStream) => Unit): Unit = {
    println(s"Analyzer => ${analyzer.getClass.getName} Start")
    for (text <- texts) {
      val reader = new StringReader(text)
      val tokenStream = analyzer.tokenStream("", reader)

      try {
        body(text, tokenStream)
      } finally {
        tokenStream.close()
      }
    }
    println(s"Analyzer => ${analyzer.getClass.getName} End")
    println()
  }

  def displayTokens(text: String, tokenStream: TokenStream): Unit = {
    val charTermAttr = tokenStream.addAttribute(classOf[CharTermAttribute])
    val offsetAttr = tokenStream.addAttribute(classOf[OffsetAttribute])
    val positionIncrementAttr = tokenStream.addAttribute(classOf[PositionIncrementAttribute])
    val typeAttr = tokenStream.addAttribute(classOf[TypeAttribute])  // JapaneseAnalyzerは、これを入れないと取得できない

    // Kuromoji Additional Attributes
    val baseFormAttr = tokenStream.addAttribute(classOf[BaseFormAttribute])
    val partOfSpeechAttr = tokenStream.addAttribute(classOf[PartOfSpeechAttribute])
    val readingAttr = tokenStream.addAttribute(classOf[ReadingAttribute])
    val inflectionAttr = tokenStream.addAttribute(classOf[InflectionAttribute])

    println("<<==========================================")
    println(s"input text => $text")
    println("============================================")

    tokenStream.reset()

    while (tokenStream.incrementToken()) {
      val startOffset = offsetAttr.startOffset
      val endOffset = offsetAttr.endOffset
      val token = charTermAttr.toString
      val posInc = positionIncrementAttr.getPositionIncrement
      val tpe = typeAttr.`type`

      // Kuromoji Additional Attributes
      val baseForm = baseFormAttr.getBaseForm
      val partOfSpeech = partOfSpeechAttr.getPartOfSpeech
      val reading = readingAttr.getReading
      val pronunciation = readingAttr.getPronunciation
      val inflectionForm = inflectionAttr.getInflectionForm
      val inflectionType = inflectionAttr.getInflectionType

      println(s"token: $token, startOffset: $startOffset, endOffset: $endOffset, posInc: $posInc, type: $tpe")

      if (partOfSpeech != null) {
        println(s"baseForm: $baseForm, partOfSpeech: $partOfSpeech, reading: $reading, pronunciation: $pronunciation, inflectionForm: $inflectionForm, inflectionType: $inflectionType")
      }
    }

    tokenStream.end()

    println("==========================================>>")
  }
}
