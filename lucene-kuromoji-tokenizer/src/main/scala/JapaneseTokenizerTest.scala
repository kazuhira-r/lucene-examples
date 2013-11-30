import java.io.StringReader

import org.apache.lucene.analysis.{Tokenizer, TokenStream}
import org.apache.lucene.analysis.ja.JapaneseTokenizer
import org.apache.lucene.analysis.ja.dict.UserDictionary
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

object JapaneseTokenizerTest {
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
    } withJapaneseTokenizer(text, mode)(displayTokens)
  }

  def withJapaneseTokenizer(text: String, mode: JapaneseTokenizer.Mode)(body: (String, TokenStream) => Unit): Unit = {
    val tokenizer = new JapaneseTokenizer(new StringReader(text),
                                          null,
                                          true,
                                          mode)

    println(s"Mode => $mode Start")

    try {
      body(text, tokenizer)
    } finally {
      tokenizer.close()
    }

    println(s"Mode => $mode End")
    println()
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
