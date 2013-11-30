import java.io.{InputStreamReader, StringReader}

import org.apache.lucene.analysis.{Analyzer, Tokenizer, TokenStream}
import org.apache.lucene.analysis.ja.{JapaneseAnalyzer, JapaneseTokenizer}
import org.apache.lucene.analysis.ja.dict.UserDictionary
import org.apache.lucene.analysis.ja.tokenattributes.{BaseFormAttribute, PartOfSpeechAttribute, ReadingAttribute, InflectionAttribute}
import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, OffsetAttribute, PositionIncrementAttribute, TypeAttribute}
import org.apache.lucene.util.Version

object KuromojiUserDict {
  def main(args: Array[String]): Unit = {
    val texts = List(
      "かずひらは、はてなダイアリーを使用しています。",
      "東京メトロ丸ノ内線は、今日も混んでいます。",
      "関西国際空港は、日本の空港です。"
    )

    for (text <- texts) {
      withJapaneseAnalyzer(text, JapaneseTokenizer.Mode.SEARCH)(displayTokens)
    }
  }

  def createUserDictionary(): UserDictionary = {
    val reader =
      new InputStreamReader(this.getClass.getResourceAsStream("my-userdict.txt"))
    try {
      // UserDirectoryの内部で、BufferedReaderに包んでいる
      new UserDictionary(reader)
    } finally {
      reader.close()
    }
  }

  def withJapaneseAnalyzer(text: String, mode: JapaneseTokenizer.Mode)(body: (String, TokenStream) => Unit): Unit = {
    val analyzer = new JapaneseAnalyzer(Version.LUCENE_43,
                                        createUserDictionary(),
                                        mode,
                                        JapaneseAnalyzer.getDefaultStopSet,
                                        JapaneseAnalyzer.getDefaultStopTags)
    println(s"Mode => $mode Start")

    val reader = new StringReader(text)
    val tokenStream = analyzer.tokenStream("", reader)

    try {
      body(text, tokenStream)
    } finally {
      tokenStream.close()
    }

    println(s"Mode => $mode End")
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
