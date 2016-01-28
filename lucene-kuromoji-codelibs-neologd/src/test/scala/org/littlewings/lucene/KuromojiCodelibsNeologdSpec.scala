package org.littlewings.lucene

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseAnalyzer
import org.scalatest.{FunSpec, Matchers}

class KuromojiCodelibsNeologdSpec extends FunSpec with Matchers {
  describe("Kuromoji Codelibs Neologd Spec") {
    it("simple usage.") {
      val targetWord = "ゲスの極み乙女。もモーニング娘。も問題なく分割できます。"

      val analyzer = new JapaneseAnalyzer
      val tokenStream = analyzer.tokenStream("", targetWord)

      val charTermAttr = tokenStream.addAttribute(classOf[CharTermAttribute])

      tokenStream.reset()

      val tokens = Iterator
        .continually(tokenStream.incrementToken())
        .takeWhile(identity)
        .map(_ => charTermAttr.toString)
        .toVector

      tokenStream.end()
      tokenStream.close()

      tokens should contain inOrderOnly("ゲスの極み乙女。", "モーニング娘。", "問題", "分割")
    }
  }
}
