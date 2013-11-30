import scala.collection.JavaConverters._

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.search.{IndexSearcher, Query, Sort, SortField, TermQuery, TopFieldCollector}
import org.apache.lucene.search.{BooleanClause, BooleanQuery, MatchAllDocsQuery, TermQuery}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

import org.apache.lucene.search.join.{JoinUtil, ScoreMode}

object LuceneJoin {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_44
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    for {
      bookDirectory <- new RAMDirectory
      detailDirectory <- new RAMDirectory
      categoryDirectory <- new RAMDirectory
    } {
      registryBooks(bookDirectory, luceneVersion, analyzer)
      registryBookDetails(detailDirectory, luceneVersion, analyzer)
      registryCategories(categoryDirectory, luceneVersion, analyzer)

      val bookAndDetailFromQuery = new BooleanQuery
      bookAndDetailFromQuery.add(new TermQuery(new Term("isbn13", "978-4844330844")),
                    BooleanClause.Occur.SHOULD)
      bookAndDetailFromQuery.add(new TermQuery(new Term("isbn13", "978-4274069130")),
                    BooleanClause.Occur.SHOULD)

      join(label = "===== Join Book And Detail =====",
           fromDirectory = bookDirectory,
           toDirectory = detailDirectory,
           fromQuery = bookAndDetailFromQuery,
           fromField = "isbn13",
           toField = "isbn13-ref",
           luceneVersion = luceneVersion,
           analyzer = analyzer)

      val categoryAndBookFromQuery = new BooleanQuery
      categoryAndBookFromQuery.add(new TermQuery(new Term("category", "scala")), BooleanClause.Occur.SHOULD)
      categoryAndBookFromQuery.add(new TermQuery(new Term("category", "clojure")), BooleanClause.Occur.SHOULD)

      join(label = "===== Join Category And Book =====",
           fromDirectory = categoryDirectory,
           toDirectory = bookDirectory,
           fromQuery = categoryAndBookFromQuery,
           fromField = "category",
           toField = "category-ref",
           luceneVersion = luceneVersion,
           analyzer = analyzer)
    }
  }

  private def join(label: String,
                   fromDirectory: Directory,
                   toDirectory: Directory,
                   fromQuery: Query,
                   fromField: String,
                   toField: String,
                   luceneVersion: Version,
                   analyzer: Analyzer): Unit =
    for {
      fromReader <- DirectoryReader.open(fromDirectory)
      toReader <- DirectoryReader.open(toDirectory)
    } {
      val fromSearcher = new IndexSearcher(fromReader)
      val toSearcher = new IndexSearcher(toReader)

      val multipleValuesPerDoc = false

      val joinQuery = JoinUtil.createJoinQuery(fromField,
                                               multipleValuesPerDoc,
                                               toField,
                                               fromQuery,
                                               fromSearcher,
                                               ScoreMode.Max)

      val docCollector = TopFieldCollector.create(Sort.RELEVANCE,
                                                       100,
                                                       true,
                                                       false,
                                                       false,
                                                       false)

      toSearcher.search(joinQuery, docCollector)

      println(label)
      for (h <- docCollector.topDocs.scoreDocs) {
        val hitDoc = toSearcher.doc(h.doc)
        println { s"Score,N[${h.score}:${h.doc}] : Doc => " +
                  hitDoc
                    .getFields
                    .asScala
                    .map(_.stringValue)
                    .mkString(" ", " | ", "")
                }
      }
    }

  private def registryBooks(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (indexWriter <- new IndexWriter(directory,
                                        new IndexWriterConfig(luceneVersion, analyzer))) {
      indexWriter.addDocument(book("978-4894714991",
                                   "Effective Java 第2版",
                                   "java",
                                   3780))
      indexWriter.addDocument(book("978-4774139906",
                                   "パーフェクトJava",
                                   "java",
                                   3780))
      indexWriter.addDocument(book("978-4844330844",
                                   "Scalaスケーラブルプログラミング第2版",
                                   "scala",
                                   4830))
      indexWriter.addDocument(book("978-4798125411",
                                   "Scala逆引きレシピ (PROGRAMMER’S RECiPE)",
                                   "scala",
                                   3360))
      indexWriter.addDocument(book("978-4274069130",
                                   "プログラミングClojure 第2版",
                                   "clojure",
                                   3570))
      indexWriter.addDocument(book("978-4774159911",
                                   "おいしいClojure入門",
                                   "clojure",
                                   2919)) 
    }

  private def book(isbn13: String, title: String, category: String, price: Int): Document = {
    val document = new Document
    document.add(new StringField("isbn13", isbn13, Field.Store.YES))
    document.add(new TextField("title", title, Field.Store.YES))
    document.add(new StringField("category-ref", category, Field.Store.YES))
    document.add(new StringField("price", price.toString, Field.Store.YES))
    document
  }

  private def registryBookDetails(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (indexWriter <- new IndexWriter(directory,
                                        new IndexWriterConfig(luceneVersion, analyzer))) {
      indexWriter.addDocument(bookDetail("978-4894714991",
                                         "Effective Java 第2版",
                                         "2008",
                                         "11"))
      indexWriter.addDocument(bookDetail("978-4774139906",
                                         "パーフェクトJava",
                                         "2009",
                                         "09"))
      indexWriter.addDocument(bookDetail("978-4844330844",
                                         "Scalaスケーラブルプログラミング第2版",
                                         "2011",
                                         "09"))
      indexWriter.addDocument(bookDetail("978-4798125411",
                                         "Scala逆引きレシピ (PROGRAMMER’S RECiPE)",
                                         "2012",
                                         "07"))
      indexWriter.addDocument(bookDetail("978-4274069130",
                                         "プログラミングClojure 第2版",
                                         "2013",
                                         "04"))
      indexWriter.addDocument(bookDetail("978-4774159911",
                                         "おいしいClojure入門",
                                         "2013",
                                         "09"))
    }

  private def bookDetail(isbn13: String, title: String, year: String, month: String): Document = {
    val document = new Document
    document.add(new StringField("isbn13-ref", isbn13, Field.Store.YES))
    document.add(new TextField("title", title, Field.Store.YES))
    document.add(new StringField("year", year, Field.Store.YES))
    document.add(new StringField("month", month, Field.Store.YES))
    document
  }

  private def registryCategories(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (indexWriter <- new IndexWriter(directory,
                                        new IndexWriterConfig(luceneVersion, analyzer))) {
      indexWriter.addDocument(category("java", "Object Oriented Programming Language"))
      indexWriter.addDocument(category("scala", "OOP and Functionaly Programming Language"))
      indexWriter.addDocument(category("clojure", "Functionaly Programming Language"))
    }

  private def category(category: String, explain: String): Document = {
    val document = new Document
    document.add(new StringField("category", category, Field.Store.YES))
    document.add(new TextField("explain", explain, Field.Store.YES))
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
