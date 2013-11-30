import scala.collection.JavaConverters._

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery, MultiCollector, Query, TermQuery, TopScoreDocCollector}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.Version

import org.apache.lucene.facet.index.FacetFields
import org.apache.lucene.facet.params.FacetSearchParams
import org.apache.lucene.facet.search.{CountFacetRequest, FacetRequest, FacetResultNode, FacetsAccumulator, FacetsCollector}
import org.apache.lucene.facet.taxonomy.{CategoryPath, TaxonomyWriter}
import org.apache.lucene.facet.taxonomy.directory.{DirectoryTaxonomyReader, DirectoryTaxonomyWriter}

object LuceneFacet {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_44
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    val indexDirectory = new RAMDirectory
    val taxonomyDirectory = new RAMDirectory

    registryDocuments(indexDirectory, taxonomyDirectory, luceneVersion, analyzer)

    facetQuery(indexDirectory, taxonomyDirectory, luceneVersion)

    indexDirectory.close()
    taxonomyDirectory.close()
  }

  private def registryDocuments(indexDirectory: Directory,
                                taxonomyDirectory: Directory,
                                luceneVersion: Version,
                                analyzer: Analyzer): Unit = {
    val writer = new IndexWriter(indexDirectory,
                                 new IndexWriterConfig(luceneVersion, analyzer))
    val taxonomyWriter = new DirectoryTaxonomyWriter(taxonomyDirectory)
    val facetFields = new FacetFields(taxonomyWriter)

    writer.addDocument(createDocument(taxonomyWriter,
                                      Book("Effective Java 第2版",
                                           Array("Joshua Bloch", "柴田芳樹"),
                                           "2008",
                                           "11",
                                           3780,
                                           "java"),
                                      facetFields))
    writer.addDocument(createDocument(taxonomyWriter,
                                      Book("パーフェクトJava",
                                           Array("アリエル・ネットワーク株式会社", "井上 誠一郎", "永井 雅人", "松山 智大"),
                                           "2009",
                                           "09",
                                           3780,
                                           "java"),
                                      facetFields))
    writer.addDocument(createDocument(taxonomyWriter,
                                      Book("やさしいJava 第5版",
                                           Array("高橋 麻奈"),
                                           "2013",
                                           "08",
                                           2730,
                                           "java"),
                                      facetFields))
    writer.addDocument(createDocument(taxonomyWriter,
                                      Book("Java言語プログラミングレッスン 第3版(上)",
                                           Array("結城 浩"),
                                           "2013",
                                           "11",
                                           2520,
                                           "java"),
                                      facetFields))
    writer.addDocument(createDocument(taxonomyWriter,
                                      Book("Java言語プログラミングレッスン 第3版(下)",
                                           Array("結城 浩"),
                                           "2013",
                                           "11",
                                           2520,
                                           "java"),
                                      facetFields))
    writer.addDocument(createDocument(taxonomyWriter,
                                      Book("Scalaスケーラブルプログラミング第2版",
                                           Array("Martin Odersky", "Lex Spoon", "Bill Venners", "羽生田 栄一", "水島 宏太", "長尾 高弘"),
                                           "2011",
                                           "09",
                                           4830,
                                           "scala"),
                                      facetFields))
    writer.addDocument(createDocument(taxonomyWriter,
                                      Book("Scala逆引きレシピ (PROGRAMMER’S RECiPE)",
                                           Array("竹添 直樹", "島本 多可子"),
                                           "2012",
                                           "07",
                                           3360,
                                           "scala"),
                                      facetFields))
    writer.addDocument(createDocument(taxonomyWriter,
                                      Book("Scalaプログラミング入門",
                                           Array("デイビッド・ポラック", "羽生田栄一", "大塚庸史"),
                                           "2010",
                                           "03",
                                           3360,
                                           "scala"),
                                      facetFields))
    writer.addDocument(createDocument(taxonomyWriter,
                                      Book("プログラミングScala",
                                           Array("Dean Wampler", "Alex Payne", "株式会社オージス総研 オブジェクトの広場編集部"),
                                           "2011",
                                           "01",
                                           3990,
                                           "scala"),
                                      facetFields))


    writer.close()
    taxonomyWriter.close()
  }

  private def createDocument(taxonomyWriter: TaxonomyWriter ,book: Book, facetFields: FacetFields): Document = {
    val document = new Document

    // Build Document
    document.add(new TextField("title", book.title, Field.Store.YES))
    document.add(new TextField("authors", book.authors.mkString(", "), Field.Store.YES))
    document.add(new StringField("year", book.year, Field.Store.YES))
    document.add(new StringField("month", book.month, Field.Store.YES))
    document.add(new StringField("price", book.price.toString, Field.Store.YES))
    document.add(new StringField("language", book.language, Field.Store.YES))

    // Build Category
    val categoryPaths = List(
      new CategoryPath("publishTime", book.year, book.month),
      new CategoryPath("price", book.price.toString),
      new CategoryPath("language", book.language)
    ) ::: book.authors.map(n => new CategoryPath("author", n)).toList
 
    facetFields.addFields(document, categoryPaths.asJava)

    document
  }

  private def facetQuery(indexDirectory: Directory, taxonomyDirectory: Directory, luceneVersion: Version): Unit = {
    val reader = DirectoryReader.open(indexDirectory)
    val taxonomyReader = new DirectoryTaxonomyReader(taxonomyDirectory)

    val searcher = new IndexSearcher(reader)

    def searchFacet[T](query: Query, categoryPath: CategoryPath, num: Int)
                      (fun: FacetResultNode => Unit): Unit = {
      val topScoreDocCollector = TopScoreDocCollector.create(100, true)
      val facetSearchParams =
        new FacetSearchParams(
          new CountFacetRequest(categoryPath, num))

      val facetsCollector = FacetsCollector.create(new FacetsAccumulator(facetSearchParams,
                                                                         reader,
                                                                         taxonomyReader))

      searcher.search(query, MultiCollector.wrap(topScoreDocCollector, facetsCollector))

      for {
        facetResult <- facetsCollector.getFacetResults.asScala
        subResult <- facetResult.getFacetResultNode.subResults.asScala
      } {
        fun(subResult)
      }
    }


    val fun: String => FacetResultNode => Unit =
      caseName => {
        println(s"Case[$caseName]")
        subResult =>
          println(s"   label: ${subResult.label}, value: ${subResult.value}")
      }

    searchFacet(new MatchAllDocsQuery,
                new CategoryPath("publishTime"), 10)(fun("Query => allDocs, Facet => publishTime"))
    searchFacet(new MatchAllDocsQuery,
                new CategoryPath("publishTime"), 1)(fun("Query => allDocs, Facet => publishTime, num 1"))
    searchFacet(new TermQuery(new Term("language", "java")),
                new CategoryPath("publishTime", "2008"), 10)(fun("Query => language:java, Facet => publishTime/2008"))
    searchFacet(new TermQuery(new Term("language", "scala")),
                new CategoryPath("publishTime"), 10)(fun("Query => language:scala, Facet => publishTime"))
    searchFacet(new MatchAllDocsQuery,
                new CategoryPath("language"), 10)(fun("Query => allDocs, Facet => language"))
    searchFacet(new TermQuery(new Term("language", "scala")),
                new CategoryPath("publishTime", "2011"), 10)(fun("Query => language:scala, Facet => publishTime/2011"))
    searchFacet(new MatchAllDocsQuery,
                new CategoryPath("author"), 20)(fun("Query => allDocs, Facet => author"))


    reader.close()
    taxonomyReader.close()
  }
}

case class Book(title: String,
                authors: Array[String],
                year: String,
                month: String,
                price: Int,
                language: String)
