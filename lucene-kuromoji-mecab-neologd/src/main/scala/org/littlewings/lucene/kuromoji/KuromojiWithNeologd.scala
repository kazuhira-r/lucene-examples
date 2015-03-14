package org.littlewings.lucene.kuromoji

import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

object KuromojiWithNeologd {
  def main(args: Array[String]): Unit = {
    val texts = Array(
      "すもももももももものうち",
      "きゃりーぱみゅぱみゅ",
      "日本経済新聞でモバゲーの記事を読んだ",
      "くりーむしちゅー",
      "艦隊これくしょん"
    )

    val analyzer = new JapaneseAnalyzer

    for (text <- texts) {
      val tokenStream = analyzer.tokenStream("", text)

      val charTermAttr = tokenStream.addAttribute(classOf[CharTermAttribute])

      tokenStream.reset()

      val tokens =
        Iterator
          .continually(tokenStream.incrementToken())
          .takeWhile(identity)
          .map(_ => charTermAttr.toString)

      println(s"InputText = $text")
      println(s"  Tokenized = ${tokens.mkString("[", ", ", "]")}")

      tokenStream.close()
    }
  }
}
