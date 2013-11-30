import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.util.Version

object LuceneKuromojiDict {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_45
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    for (text <- Array("すもももももももものうち。",
                       "メガネは顔の一部です。",
                       "日本経済新聞でモバゲーの記事を読んだ。",
                       "Java, Scala, Groovy, Clojure",
                       "ＬＵＣＥＮＥ、ＳＯＬＲ、Lucene, Solr",
                       "ｱｲｳｴｵカキクケコさしすせそABCＸＹＺ123４５６",
                       "Lucene is a full-featured text search engine library written in Java.",
                       "このComputerは、10回に1回の割合で起動中に青い画面が表示されます。")) {
      println(s"Original[$text]")

      val tokenStream = analyzer.tokenStream("", text)
      val charTermAttribute = tokenStream.addAttribute(classOf[CharTermAttribute])

      tokenStream.reset()

      println {
        Iterator
          .continually(tokenStream)
          .takeWhile(_.incrementToken())
          .map(t => charTermAttribute.toString)
            .mkString("Tokenize[", " ", "]")
      }

      tokenStream.close()

      println()
    }
  }
}
