import scala.collection.JavaConverters._

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, FieldType, TextField, StringField}
import org.apache.lucene.index.{DirectoryReader, DocsAndPositionsEnum, FieldInfo, IndexWriter, IndexWriterConfig}
import org.apache.lucene.index.{Term, TermsEnum}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

object LuceneTermVector {
  def main(args: Array[String]): Unit =
    for (directory <- new RAMDirectory) {
      val luceneVersion = Version.LUCENE_44
      val analyzer = new JapaneseAnalyzer(luceneVersion)

      registryDocuments(directory, luceneVersion, analyzer)

      printTermVectors(directory)
    }

  private def printTermVectors(directory: Directory): Unit =
    for {
      reader <- DirectoryReader.open(directory)
      id <- (0 until reader.maxDoc)
    } {
      // IndexReader#getTermVector(id, fieldName)とする方法もある
      // その場合、Termsが返却される
      val termVectors = reader.getTermVectors(id)
      termVectors.iterator.asScala.foreach { field =>
        val terms = termVectors.terms(field)
        val reuse: TermsEnum = null

        val termsEnum = terms.iterator(reuse)

        println(s"Doc[$id] Field: $field")
        println("  TermsEnum#totalTermFreq: TermsEnum#doqFreq: IndexReader#docFreq: value")
        Iterator
          .continually(termsEnum.next)
          .takeWhile(_ != null)
          .foreach { bytesRef =>
            // （ドキュメント内の単語出現回数）
            print(s"    ${termsEnum.totalTermFreq}: ")
            // （ドキュメント内に単語があれば、1？）
            print(s"${termsEnum.docFreq}: ")
            // （単語の出現するドキュメント数）
            print(s"${reader.docFreq(new Term(field, bytesRef.utf8ToString))}: ")
            // 単語
            println(s"${bytesRef.utf8ToString}")
          }

        /* DocsAndPositionsEnumを使用する場合は、こちら
        println("  DocsAndPositionsEnum#freq: value")
        Iterator
          .continually(termsEnum.next)
          .takeWhile(_ != null)
          .foreach { bytesRef =>
            var docsAndPositions: DocsAndPositionsEnum = null

            docsAndPositions = termsEnum.docsAndPositions(null, docsAndPositions)
            if (docsAndPositions.nextDoc != 0) {
              throw new IllegalStateException("you need to call nextDoc() to have the enum positioned")
            }

            // ドキュメント内の単語の出現回数
            val freq = docsAndPositions.freq

            println(s"    $freq: ${bytesRef.utf8ToString}")

            println("      position: startOffset-endOffset")
            for (i <- 1 to freq) {
              // Termの位置
              val position = docsAndPositions.nextPosition
              // Termの出現位置のオフセット（文字単位）
              val startOffset = docsAndPositions.startOffset
              val endOffset = docsAndPositions.endOffset
              println(s"        $position: ${startOffset}-${endOffset}")
            }
          }
        */
      }
    }

  private def registryDocuments(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (indexWriter <- new IndexWriter(directory,
                                        new IndexWriterConfig(luceneVersion, analyzer))) {
      indexWriter.addDocument(createBook("978-4774127804",
                                         "Apache Lucene 入門 ～Java・オープンソース・全文検索システムの構築",
                                         3360,
                                         "Luceneは全文検索システムを構築するためのJavaのライブラリです。Luceneを使えば,一味違う高機能なWebアプリケーションを作ることができます。たとえばWeb通販・ショッピングサイト,企業サイトの情報サービス,学術系サイトでの情報サービスなどが挙げられます。Webに付加価値を与えることができるのです。本書は,全文検索システムの仕組みと機能を初心者にもわかりやすく解説し,豊富なサンプルコードで実装を示してゆきます。やさしく説明する工夫(挿話)やAjaxとLuceneを組み合わせた「インクリメンタルサーチ」など楽しい仕掛けも盛りだくさん!非常に奥深い世界が広がっています。本書は6章構成になっており、第1章から第4章が前編、第5章と第6章が後編となっている。前編を「基本編」、後編を「応用編」と読み替えてもよい。Luceneのアプリケーションを書くためには「基本編」である第1章から第4章を通読しておくことが望ましい。「応用編」である第5章と第6章は余力があれば読むとよいだろう。第1章は、全文検索とLuceneの基本知識の習得を目標とし、これらについて簡潔に述べている。Luceneに関しては、最初の全文検索のサンプルプログラムを作成し、その内容や動作を紹介している。サンプルプログラムの題材（全文検索のためのコンテンツ）には、日本でもっとも有名なファミリーである「サザエさん一家」を使用している。第2章は、Analyzer（アナライザー）について説明している。Analyzerは、全文検索の対象となるドキュメントテキストを分析し、単語を取り出す働きをするものである。本章では特に日本語のテキストの分析に重きを置き、JapaneseAnalyzerの解説にページ数を割いている。その後、CJKAnalyzer、StandardAnalyzerおよびその他のAnalyzerを紹介する。Luceneは全文検索に「転置索引方式」を採用している。そのため、検索の前にあらかじめ「インデックス」を作成しておかなければならない。第3章では、その「インデックス」の作成方法について説明している。その後、第4章で「インデックス」の検索方法について説明している。全文検索のサンプルデータとしては、サンプルプログラムの内容と動作の理解が進むよう、読者にとって親しみやすいと思われる「技術評論社の書籍データ」と郵便局の「大口事業所等個別番号データ」を用いた。後編の第5章では、前編で習得した知識を使って、全文検索機能を持ったWebアプリケーションを作成する。このWebアプリケーションの検索機能は、「データベース」、「HTMLファイル」、「XMLファイル」、「PDFファイル」および「Mocrosoft Wordファイル」といった「異種ドキュメント」を透過的に検索し、表示する。第6章では、「セキュリティ」、「検索質問語の強調表示」、「Ajaxを使用したインクリメンタルサーチ」等々といった、より応用的・発展的な内容を取り上げている。読者のLuceneアプリケーションにいろいろな機能を追加する際のヒントとなるだろう。なお、Appendix AにはLuceneはじめ、その他の関連ツールおよび本書のサンプルプログラムのインストールと実行方法を掲載している。"))

      indexWriter.addDocument(createBook("978-4774141756",
                                         "Apache Solr入門 ―オープンソース全文検索エンジン",
                                         3780,
                                         "Apache Solrとは,オープンソースの検索エンジンです.Apache LuceneというJavaの全文検索システムをベースに豊富な拡張性をもたせ,多くの開発者が利用できるように作られました.検索というと,Googleのシステムを使っている企業Webページが多いですが,自前の検索エンジンがあると顧客にとって本当に必要な情報を提供できます.SolrはJavaだけでなく,PHP,Ruby,Python,Perl,Javascriptなどでもプログラミング可能です.本書は検索エンジンの基礎から応用までじっくり解説します."))

      indexWriter.addDocument(createBook("978-4797352009",
                                         "集合知イン・アクション",
                                         3990,
                                         "レコメンデーションエンジンをつくるには?ブログやSNSのテキスト分析、ユーザー嗜好の予測モデル、レコメンデーションエンジン……Web 2.0の鍵「集合知」をJavaで実装しよう! 具体的なコードとともに丹念に解説します。はてなタグサーチやYahoo!日本語形態素解析を活用するサンプルも追加収録。"))

      indexWriter.addDocument(createBook("978-4873115665",
                                         "HBase",
                                         4410,
                                       "ビッグデータのランダムアクセス系処理に欠かせないHBaseについて、基礎から応用までを詳細に解説。クライアントAPI(高度な機能・管理機能)、Hadoopとの結合、アーキテクチャといった開発に関わる事項や、クラスタのモニタリング、パフォーマンスチューニング、管理といった運用の方法を、豊富なサンプルとともに解説します。日本語版ではAWS Elastic MapReduceについての付録を追加。ビッグデータに関心あるすべてのエンジニアに必携の一冊です。"))

      indexWriter.addDocument(createBook("978-4873115900",
                                         "MongoDBイン・アクション",
                                         3570,
                                         "本書はMongoDBを学びたいアプリケーション開発者やDBAに向けて、MongoDBの基礎から応用までを包括的に解説する書籍です。MongoDBの機能やユースケースの概要など基本的な事柄から、ドキュメント指向データやクエリと集計など、MongoDB APIの詳細について、さらにパフォーマンスやトラブルシューティングなど高度なトピックまで豊富なサンプルでわかりやすく解説します。付録ではデザインパターンも紹介。コードはJavaScriptとRubyで書かれていますが、PHP、Java、C++での利用についても触れています。MongoDBを使いこなしたいすべてのエンジニア必携の一冊です。"))
    }

  private def createBook(isbn13: String, title: String, price: Int, abstraction: String): Document = {
    val document = new Document
    document.add(new StringField("isbn13", isbn13, Field.Store.YES))

    val titleFieldType = new FieldType
    titleFieldType.setIndexed(true)
    titleFieldType.setStored(true)
    titleFieldType.setTokenized(true)
    titleFieldType.setStoreTermVectors(true)
    titleFieldType.setStoreTermVectorOffsets(true)
    titleFieldType.setStoreTermVectorPositions(true)
    document.add(new Field("title", title, titleFieldType))

    document.add(new StringField("price", price.toString, Field.Store.YES))

    val abstractionFieldType = new FieldType
    abstractionFieldType.setIndexed(true)
    abstractionFieldType.setStored(true)
    abstractionFieldType.setTokenized(true)
    abstractionFieldType.setStoreTermVectors(true)
    abstractionFieldType.setStoreTermVectorOffsets(true)
    abstractionFieldType.setStoreTermVectorPositions(true)
    document.add(new Field("abstraction", abstraction, abstractionFieldType))

    document
  }

  implicit class AutoCloseableWrapper[A <: AutoCloseable](val underlying: A) extends AnyVal {
    def foreach(fun: A => Unit): Unit =
      try {
        fun(underlying)
      } finally {
        underlying.close()
      }
  }
}
