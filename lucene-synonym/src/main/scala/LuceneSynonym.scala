import java.io.{InputStreamReader, Reader, StringReader}
import java.nio.charset.StandardCharsets

import org.apache.lucene.analysis.{Analyzer, Tokenizer, TokenStream}
import org.apache.lucene.analysis.core.{LowerCaseFilter, StopFilter, WhitespaceAnalyzer}
import org.apache.lucene.analysis.ja.{JapaneseAnalyzer, JapaneseTokenizer}
import org.apache.lucene.analysis.synonym.{SolrSynonymParser, SynonymFilter, SynonymMap}
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.util.{CharsRef, Version}

object LuceneSynonym {
  def main(args: Array[String]): Unit = {
    val synonymMap = args.toList.headOption match {
      case Some("solr") => createSynonymMapBySolrParser("synonym.txt")
      case _ => createSynonymMap
    }

    val analyzer =
      new SynonymMapAnalyzer({ reader => new JapaneseTokenizer(reader,
                                                               null,
                                                               true,
                                                               JapaneseTokenizer.Mode.SEARCH)},
                             synonymMap)

    for (str <- Array("iPhone",
                      "iphone",
                      "アイフォン",
                      "Android",
                      "アンドロイド",
                      "Hello World",
                      "hello world",
                      "Hello Another World",
                      "expand",
                      "duplicate",
                      "dup")) {
      val tokenStream = analyzer.tokenStream("", new StringReader(str))

      println(s"========== Analyze Token[$str] START ==========")

      try {
        val charTermAttr = tokenStream.addAttribute(classOf[CharTermAttribute])

        tokenStream.reset()

        Iterator
          .continually(tokenStream)
          .takeWhile(_.incrementToken())
          .foreach { ts => println(s"token: ${charTermAttr.toString}") }

        tokenStream.end()
      } finally {
        tokenStream.close()
      }

      println(s"========== Analyze Token[$str] END ==========")
    }
  }

  private def createSynonymMap: SynonymMap = {
    val builder = new SynonymMap.Builder(true)
    builder.add(SynonymMap.Builder.join(Array("iPhone"), new CharsRef),
                SynonymMap.Builder.join(Array("あいふぉん", "アイフォーン", "アイフォン"), new CharsRef),
                true)
    builder.add(SynonymMap.Builder.join(Array("Android"), new CharsRef),
                SynonymMap.Builder.join(Array("あんどろいど", "アンドロイド", "ドロイド"), new CharsRef),
                false)
    builder.add(SynonymMap.Builder.join(Array("Hello", "World"), new CharsRef),
                SynonymMap.Builder.join(Array("こんにちは", "世界", "！", "ようこそ", "いらっしゃいませ"), new CharsRef),
                true)
    builder.add(SynonymMap.Builder.join(Array("expand"), new CharsRef),
                SynonymMap.Builder.join(Array("展開します", "展開されます"), new CharsRef),
                true)
    builder.add(SynonymMap.Builder.join(Array("duplicate"), new CharsRef),
                SynonymMap.Builder.join(Array("ダブってます", "ダブリです"), new CharsRef),
                true)
    builder.add(SynonymMap.Builder.join(Array("duplicate"), new CharsRef),
                SynonymMap.Builder.join(Array("ダブってます", "ダブリです"), new CharsRef),
                true)
    builder.add(SynonymMap.Builder.join(Array("dup"), new CharsRef),
                SynonymMap.Builder.join(Array("入力単語が", "見えますが、"), new CharsRef),
                true)
    builder.add(SynonymMap.Builder.join(Array("dup"), new CharsRef),
                SynonymMap.Builder.join(Array("ダブってそうに", "これは、重複になりません"), new CharsRef),
                true)
    builder.build
  }

  private def createSynonymMapBySolrParser(path: String): SynonymMap = {
    val parser = new SolrSynonymParser(true, false, new WhitespaceAnalyzer(Version.LUCENE_43))
    parser.add(new InputStreamReader(getClass.getResourceAsStream(path),
                                     StandardCharsets.UTF_8))
    parser.build
  }
}

class SynonymMapAnalyzer(tokenizerFactory: Reader => Tokenizer, synonymMap: SynonymMap) extends Analyzer {
  override def createComponents(fieldName: String, reader: Reader): Analyzer.TokenStreamComponents = {
    val tokenizer = tokenizerFactory(reader)
    val stream = new SynonymFilter(tokenizer, synonymMap, false)
    new Analyzer.TokenStreamComponents(tokenizer, stream)
  }
}

