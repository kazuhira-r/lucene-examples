package org.littlewings.lucene.kuromoji

import scala.language.postfixOps
import scala.sys.process._
import java.io.{ByteArrayInputStream, File, StringReader}
import java.nio.charset.StandardCharsets

import org.apache.lucene.analysis.ja.{GraphvizFormatter, JapaneseTokenizer}
import org.apache.lucene.analysis.ja.dict.ConnectionCosts
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

import scala.language.postfixOps

object KuromojiNBest {
  def main(args: Array[String]): Unit = {
    val targets = Array(
      ("デジタル一眼レフ", Seq(0, 2000)),
      ("水性ボールペン", Seq(0, 2000, 5677)),
      ("ボールペン", Seq(0, 5677))
    )

    targets.foreach {
      case (word, costs) => costs.foreach(cost => displayNBestAndViterbi(word, cost))
    }
  }

  def displayNBestAndViterbi(target: String, nbestCost: Int): Unit = {
    val graphvizFormatter = new GraphvizFormatter(ConnectionCosts.getInstance)

    val tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.NORMAL)
    tokenizer.setReader(new StringReader(target))
    tokenizer.setNBestCost(nbestCost)
    tokenizer.setGraphvizFormatter(graphvizFormatter)

    val charTermAttr = tokenizer.addAttribute(classOf[CharTermAttribute])

    tokenizer.reset()

    val words =
      Iterator
        .continually(tokenizer.incrementToken())
        .takeWhile(identity)
        .map(_ => charTermAttr.toString)
        .toArray

    tokenizer.end()
    tokenizer.close()

    println {
      s"""|Input = ${
        target
      }, NBest = ${
        nbestCost
      }
          |${
        words.map(w => "  " + w).mkString(System.lineSeparator())
      }""".stripMargin
    }

    val dotOutput = graphvizFormatter.finish()
    "dot -Tgif" #< new ByteArrayInputStream(dotOutput.getBytes(StandardCharsets.UTF_8)) #> new File(s"target/${
      target
    }.gif") !
  }
}
