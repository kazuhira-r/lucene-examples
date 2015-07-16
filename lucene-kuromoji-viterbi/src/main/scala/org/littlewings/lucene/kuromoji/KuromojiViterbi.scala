package org.littlewings.lucene.kuromoji

import scala.language.postfixOps
import scala.sys.process._

import java.io.{File, ByteArrayInputStream, StringReader}
import java.nio.charset.StandardCharsets

import org.apache.lucene.analysis.ja.dict.ConnectionCosts
import org.apache.lucene.analysis.ja.{GraphvizFormatter, JapaneseTokenizer}

object KuromojiViterbi {
  def main(args: Array[String]): Unit = {
    val word = args.toList.headOption.getOrElse("すもももももももものうち")

    val graphvizFormatter = new GraphvizFormatter(ConnectionCosts.getInstance)

    val tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.NORMAL)
    tokenizer.setReader(new StringReader(word))
    tokenizer.setGraphvizFormatter(graphvizFormatter)

    tokenizer.reset()

    Iterator.continually(tokenizer.incrementToken()).takeWhile(_ == true).foreach(_ => ())

    tokenizer.end()
    tokenizer.close()

    val dotOutput = graphvizFormatter.finish()

    "dot -Tgif" #< new ByteArrayInputStream(dotOutput.getBytes(StandardCharsets.UTF_8)) #> new File("out.gif") !!
  }
}
