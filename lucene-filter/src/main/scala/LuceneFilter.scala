import scala.collection.JavaConverters._

import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.queries._
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.{BytesRef, Version}

object LuceneFilter {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_43
    val directory = new RAMDirectory
    try {
      createIndex(directory, luceneVersion)

      val allQuery = new MatchAllDocsQuery

      val limit = 20
      openWithSearch(directory) { searcher =>
        filters.foreach { filter =>
          val name = filter.getClass.getSimpleName match {
            case "" => filter.getClass.getSuperclass.getSimpleName
            case n => n
          }

          println(s"=========== ${name} を適用、 Filter => $filter ===========")
          printSearchResult(searcher,
                            searcher.search(allQuery, filter, limit),
                            limit)
          println()
        }
      }
    } finally {
      directory.close()
    }
  }

  private val filters: List[Filter] =
    List(
      // 旧RangeFilter？
      // 範囲で絞り込み
      FieldCacheRangeFilter.newStringRange("price", "3360", "3780", true, false),

      // Termを使うFilter？
      // 挙動がよくわかりません…Tokenizeしたものと、相性悪い？？
      new FieldCacheTermsFilter("price", "3780", "4830"),

      // クエリでヒットしたドキュメントのIDを、キャッシュするFilter
      new FieldValueFilter("price"),  // nagate を false にすると、結果が反転する

      // Prefix検索と同様
      new PrefixFilter(new Term("title", "groovy")),

      // 別のQueryをラップするFilter
      new QueryWrapperFilter({
        val b = new BooleanQuery
        b.add(new TermQuery(new Term("title", new BytesRef("プログラミング"))), BooleanClause.Occur.MUST)
        b.add(new TermQuery(new Term("title", "groovy")), BooleanClause.Occur.MUST)
        b
      }),

      // 他のFilterをBooleanQueryのようにラップするFilter
      {
        val bf = new BooleanFilter
        bf.add(new TermsFilter(new Term("title", "スケーラブルプログラミング")), BooleanClause.Occur.MUST)
        bf.add(new TermsFilter(new Term("title", "scala")), BooleanClause.Occur.MUST)
        bf
      },

      // 引数のFilterの結果をキャッシュするFilter
      new CachingWrapperFilter({
        val bf = new BooleanFilter
        bf.add(new TermsFilter(new Term("title", "スケーラブルプログラミング")), BooleanClause.Occur.MUST)
        bf.add(new TermsFilter(new Term("title", "scala")), BooleanClause.Occur.MUST)
        bf
      })
    )

  private def createIndex(directory: Directory, luceneVersion: Version): Unit = {
    val config =
      new IndexWriterConfig(luceneVersion, new JapaneseAnalyzer(luceneVersion))

    val writer = new IndexWriter(directory, config)
    try {
      documents.foreach(writer.addDocument)
    } finally {
      writer.close()
    }
  }

  private val documents: List[Document] =
    List(
      bookDoc("978-4894714991", "Effective Java 第2版", 3780, "2008/11/27"),
      bookDoc("978-4844330844", "Scalaスケーラブルプログラミング第2版", 4830, "2011/9/27"),
      bookDoc("978-4774147277", "プログラミングGROOVY", 3360, "2011/07/06"),
      bookDoc("978-4274069130", "プログラミングClojure 第2版", 3570, "2013/04/26")
    )

  private def bookDoc(isbn13: String, title: String, price: Int, publishDate: String): Document = {
    val doc = new Document
    doc.add(new StringField("isbn13", isbn13, Field.Store.YES))
    doc.add(new TextField("title", title, Field.Store.YES))
    doc.add(new StringField("price", price.toString, Field.Store.YES))
    doc.add(new StringField("publishDate", publishDate, Field.Store.YES))
    doc
  }

  private def openWithSearch[A](directory: Directory)(f: IndexSearcher => A): A = {
    val reader = DirectoryReader.open(directory)
    try {
      val searcher = new IndexSearcher(reader)
      f(searcher)
    } finally {
      reader.close()
    }
  }

  private def printSearchResult(searcher: IndexSearcher, docs: TopDocs, limit: Int): Unit =
    docs.scoreDocs.take(limit).foreach { h =>
      val hitDoc = searcher.doc(h.doc)
      hitDoc
        .getFields
        .asScala
        .map(_.stringValue)
        .mkString(s"  Score,DocNo[${h.score},${h.doc}] ", " | ", System.lineSeparator)
        .foreach(print)
    }
}
