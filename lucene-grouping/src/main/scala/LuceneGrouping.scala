import scala.collection.JavaConverters._

import java.nio.charset.StandardCharsets
import java.util.Collection

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery, MultiCollector, Query, Sort, SortField}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.{BytesRef, Version}

import org.apache.lucene.search.grouping.{GroupingSearch, SearchGroup, TopGroups}
import org.apache.lucene.search.grouping.term.{TermAllGroupsCollector, TermFirstPassGroupingCollector, TermSecondPassGroupingCollector}
import org.apache.lucene.search.grouping.term.TermGroupFacetCollector

object LuceneGrouping {
  def main(args: Array[String]): Unit = {
    val luceneVersion = Version.LUCENE_44
    val analyzer = new JapaneseAnalyzer(luceneVersion)

    for (directory <- new RAMDirectory) {
      registryDocuments(directory, luceneVersion, analyzer)

      groupingQuerySimply(directory, luceneVersion, analyzer)
      groupingQueryPrimitive(directory, luceneVersion, analyzer)
      groupingQueryFacet(directory, luceneVersion, analyzer)
    }
  }

  private def groupingQuerySimply(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (reader <- DirectoryReader.open(directory)) {
      val indexSearcher = new IndexSearcher(reader)

      val groupField = "language"
      val groupSort = new Sort(new SortField("language",
                                             SortField.Type.STRING,
                                             false))
      // val groupSort = Sort.RELEVANCE
      val fillSortFields = true
      val requiredTotalGroupCount = true
      val groupLimit = 100
      val docPerGroup = 100

      val groupingSearch = new GroupingSearch(groupField)
      groupingSearch
        .setGroupSort(groupSort)
        .setFillSortFields(fillSortFields)
        .setAllGroups(requiredTotalGroupCount)  // グループの数を取得するようにするかどうか
        .setGroupDocsLimit(docPerGroup)  // グループ内で、いくつドキュメントを取得するか

      val query = new MatchAllDocsQuery
      val topGroups: TopGroups[BytesRef] =
        groupingSearch.search(indexSearcher,
                              query,
                              0,
                              groupLimit)  // いくつのグループを取得するか

      println("===== groupingQuerySimply =====")

      if (requiredTotalGroupCount) {
        println(s"totalGroupCount = ${topGroups.totalGroupCount}")
      }
      println(s"totalHitCount = ${topGroups.totalHitCount}")

      for {
        group <- topGroups.groups
        h <- group.scoreDocs
      } {
        val hitDoc = indexSearcher.doc(h.doc)
        val groupValue = 
          new String(group.groupValue.bytes, StandardCharsets.UTF_8)
        println { s"Score,N[${h.score}:${h.doc}] : Group[$groupValue] Doc => " +
                  hitDoc
                    .getFields
                    .asScala
                    .map(_.stringValue)
                    .mkString(" ", " | ", "")
                }
      }
    }

  private def groupingQueryPrimitive(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (reader <- DirectoryReader.open(directory)) {
      val indexSearcher = new IndexSearcher(reader)

      val groupField = "language"
      val groupSort = new Sort(new SortField("language",
                                             SortField.Type.STRING,
                                             false))
      // val groupSort = Sort.RELEVANCE
      val fillSortFields = true
      val requiredTotalGroupCount = true
      val groupLimit = 100
      val docPerGroup = 100

      val termFirstPassGroupingCollector =
        new TermFirstPassGroupingCollector(groupField, groupSort, groupLimit)
      val termAllGroupsCollector = new TermAllGroupsCollector(groupField, 128)

      val firstCollector =
        if (requiredTotalGroupCount)
          MultiCollector.wrap(termFirstPassGroupingCollector,
                              termAllGroupsCollector)
        else termFirstPassGroupingCollector


      val query = new MatchAllDocsQuery
      indexSearcher.search(query, firstCollector)

      val searchGroups: Collection[SearchGroup[BytesRef]] =
        termFirstPassGroupingCollector.getTopGroups(0, fillSortFields)

      val withinGroupSort = new Sort(new SortField("price",
                                                   SortField.Type.STRING,
                                                   true))
      val termSecondPassGroupingCollector =
        new TermSecondPassGroupingCollector(groupField,
                                            searchGroups,
                                            groupSort,
                                            withinGroupSort,
                                            docPerGroup,
                                            false,
                                            false,
                                            fillSortFields)

      indexSearcher.search(query, termSecondPassGroupingCollector)

      val topGroups: TopGroups[BytesRef] =
        termSecondPassGroupingCollector.getTopGroups(0)

      println("===== groupingQueryPrimitive =====")

      if (requiredTotalGroupCount) {
        println(s"totalGroupCount = ${termAllGroupsCollector.getGroups.size}")
      }
      println(s"totalHitCount = ${topGroups.totalHitCount}")

      for {
        group <- topGroups.groups
        h <- group.scoreDocs
      } {
        val hitDoc = indexSearcher.doc(h.doc)
        val groupValue = 
          new String(group.groupValue.bytes, StandardCharsets.UTF_8)
        println { s"Score,N[${h.score}:${h.doc}] : Group[$groupValue] Doc => " +
                  hitDoc
                    .getFields
                    .asScala
                    .map(_.stringValue)
                    .mkString(" ", " | ", "")
                }
      }
    }

  private def groupingQueryFacet(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (reader <- DirectoryReader.open(directory)) {
      val indexSearcher = new IndexSearcher(reader)

      val groupField = "isbn13"
      val facetField = "language"
      val facetFieldMultivalued = false
      val facetPrefix: BytesRef = null
      val initialSize = 128
      val offset = 0
      val facetLimit = 100
      val minCount = 0

      val termGroupFacetCollector =
        TermGroupFacetCollector.createTermGroupFacetCollector(groupField,
                                                              facetField,
                                                              facetFieldMultivalued,
                                                              facetPrefix,
                                                              initialSize)

      val query = new MatchAllDocsQuery

      indexSearcher.search(query, termGroupFacetCollector)

      val result = termGroupFacetCollector.mergeSegmentResults(offset + facetLimit,
                                                               minCount,
                                                               false)

      println("===== groupingQueryFacet =====")
      
      println(s"Total Facet Total Count = ${result.getTotalCount}")
      println(s"Total Facet Total Missing Count = ${result.getTotalMissingCount}")

      for (facetEntry <- result.getFacetEntries(offset, facetLimit).asScala) {
        println(s"Facet Value = ${new String(facetEntry.getValue.bytes, "UTF-8")}, " +
                s"Count = ${facetEntry.getCount}")
      }
    }

  private def registryDocuments(directory: Directory, luceneVersion: Version, analyzer: Analyzer): Unit =
    for (indexWriter <- new IndexWriter(directory,
                                        new IndexWriterConfig(luceneVersion, analyzer))) {
      indexWriter.addDocument(book("978-4894714991",
                                   "Effective Java 第2版",
                                   "2008",
                                   "java",
                                   3780))
      indexWriter.addDocument(book("978-4774139906",
                                   "パーフェクトJava",
                                   "2009",
                                   "java",
                                   3780))
      indexWriter.addDocument(book("978-4844330844",
                                   "Scalaスケーラブルプログラミング第2版",
                                   "2011",
                                   "scala",
                                   4830))
      indexWriter.addDocument(book("978-4798125411",
                                   "Scala逆引きレシピ (PROGRAMMER’S RECiPE)",
                                   "2012",
                                   "scala",
                                   3360))
      indexWriter.addDocument(book("978-4822284237",
                                   "Scalaプログラミング入門",
                                   "2010",
                                   "scala",
                                   3360))
      indexWriter.addDocument(book("978-4873114811",
                                   "プログラミングScala",
                                   "2011",
                                   "scala",
                                   3990))
      indexWriter.addDocument(book("978-4774147277",
                                   "プログラミングGROOVY",
                                   "2011",
                                   "groovy",
                                   3360))
      indexWriter.addDocument(book("978-4839927271",
                                   "Groovyイン・アクション",
                                   "2008",
                                   "groovy",
                                   5800))
      indexWriter.addDocument(book("978-4274069130",
                                   "プログラミングClojure 第2版",
                                   "2013",
                                   "clojure",
                                   3570))
    }

  private def book(isbn13: String,
                   title: String,
                   year: String,
                   language: String,
                   price: Int): Document = {
    val document = new Document
    document.add(new StringField("isbn13", isbn13, Field.Store.YES))
    document.add(new TextField("title", title, Field.Store.YES))
    document.add(new StringField("year", year, Field.Store.YES))
    document.add(new StringField("language", language, Field.Store.YES))
    document.add(new StringField("price", price.toString, Field.Store.YES))
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
