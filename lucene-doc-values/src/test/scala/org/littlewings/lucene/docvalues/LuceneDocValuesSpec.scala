package org.littlewings.lucene.docvalues

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document._
import org.apache.lucene.index._
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.util.BytesRef
import org.littlewings.lucene.docvalues.LuceneDocValuesSpec.AutoCloseableWrapper
import org.scalatest.{FunSpec, Matchers}

class LuceneDocValuesSpec extends FunSpec with Matchers {
  describe("Lucene Doc-Values Spec") {
    it("using Normal Field") {
      withDirectory(directory => {
        val analyzer = new StandardAnalyzer

        addDocuments(
          directory,
          analyzer,
          createDocument(new StringField("name", "カツオ", Store.YES), new IntField("age", 11, Store.YES)),
          createDocument(new StringField("name", "ワカメ", Store.YES), new IntField("age", 9, Store.YES)),
          createDocument(new StringField("name", "タラオ", Store.YES), new IntField("age", 3, Store.YES))
        )

        for (reader <- DirectoryReader.open(directory)) {
          val searcher = new IndexSearcher(reader)
          val queryParser = new QueryParser("name", analyzer)
          val query = queryParser.parse("name: カツオ OR name: タラオ")

          val thrown1 = the[IllegalStateException] thrownBy searcher.search(query, 10, new Sort(new SortField("age", SortField.Type.INT)))
          thrown1.getMessage should be("unexpected docvalues type NONE for field 'age' (expected=NUMERIC). Use UninvertingReader or index with docvalues.")

          val topDocs = searcher.search(query, 10, Sort.RELEVANCE)
          topDocs.totalHits should be(2)
          searcher.doc(topDocs.scoreDocs(0).doc).get("name") should be("カツオ")
          searcher.doc(topDocs.scoreDocs(1).doc).get("name") should be("タラオ")

          val thrown2 = the[IllegalStateException] thrownBy searcher.search(query, 10, new Sort(new SortField("name", SortField.Type.STRING_VAL)))
          thrown2.getMessage should be("unexpected docvalues type NONE for field 'name' (expected one of [BINARY, SORTED]). Use UninvertingReader or index with docvalues.")

          val thrown3 = the[IllegalStateException] thrownBy searcher.search(query, 10, new Sort(new SortField("name", SortField.Type.STRING)))
          thrown3.getMessage should be("unexpected docvalues type NONE for field 'name' (expected=SORTED). Use UninvertingReader or index with docvalues.")
        }
      })
    }


    it("using DocValues Field, bad?") {
      withDirectory(directory => {
        val analyzer = new StandardAnalyzer

        addDocuments(
          directory,
          analyzer,
          createDocument(new BinaryDocValuesField("name", new BytesRef("カツオ")), new NumericDocValuesField("age", 11)),
          createDocument(new BinaryDocValuesField("name", new BytesRef("ワカメ")), new NumericDocValuesField("age", 9)),
          createDocument(new BinaryDocValuesField("name", new BytesRef("タラオ")), new NumericDocValuesField("age", 3))
        )

        for (reader <- DirectoryReader.open(directory)) {
          val searcher = new IndexSearcher(reader)
          val queryParser = new QueryParser("name", analyzer)
          val query = queryParser.parse("name: カツオ OR name: タラオ")

          val topDocs = searcher.search(query, 10, new Sort(new SortField("age", SortField.Type.INT)))
          topDocs.totalHits should be(0)
        }
      })
    }

    it("using DocValues Field") {
      withDirectory(directory => {
        val analyzer = new StandardAnalyzer

        addDocuments(
          directory,
          analyzer,
          createDocument(new StringField("name", "カツオ", Store.YES),
            new SortedDocValuesField("name", new BytesRef("カツオ")),
            new IntField("age", 11, Store.YES),
            new NumericDocValuesField("age", 11L)),
          createDocument(new StringField("name", "ワカメ", Store.YES),
            new SortedDocValuesField("name", new BytesRef("ワカメ")),
            new IntField("age", 9, Store.YES),
            new NumericDocValuesField("age", 9L)),
          createDocument(new StringField("name", "タラオ", Store.YES),
            new SortedDocValuesField("name", new BytesRef("タラオ")),
            new IntField("age", 3, Store.YES),
            new NumericDocValuesField("age", 3L))
        )

        for (reader <- DirectoryReader.open(directory)) {
          val searcher = new IndexSearcher(reader)
          val queryParser = new QueryParser("name", analyzer)
          val query = queryParser.parse("name: カツオ OR name: タラオ")

          val topDocs = searcher.search(query, 10, new Sort(new SortField("age", SortField.Type.INT)))
          topDocs.totalHits should be(2)
          searcher.doc(topDocs.scoreDocs(0).doc).get("name") should be("タラオ")
          searcher.doc(topDocs.scoreDocs(1).doc).get("name") should be("カツオ")

          val topDocs2 = searcher.search(query, 10, new Sort(new SortField("name", SortField.Type.STRING_VAL, true)))
          topDocs2.totalHits should be(2)
          searcher.doc(topDocs2.scoreDocs(0).doc).get("name") should be("タラオ")
          searcher.doc(topDocs2.scoreDocs(1).doc).get("name") should be("カツオ")

          val topDocs3 = searcher.search(query, 10, new Sort(new SortField("name", SortField.Type.STRING, true)))
          topDocs3.totalHits should be(2)
          searcher.doc(topDocs3.scoreDocs(0).doc).get("name") should be("タラオ")
          searcher.doc(topDocs3.scoreDocs(1).doc).get("name") should be("カツオ")
        }
      })
    }

    it("using DocValues Field2") {
      withDirectory(directory => {
        val analyzer = new StandardAnalyzer

        addDocuments(
          directory,
          analyzer,
          createDocument(new StringField("name", "カツオ", Store.YES),
            new SortedDocValuesField("name", new BytesRef("カツオ")),
            new IntField("age", 11, Store.YES),
            new SortedNumericDocValuesField("age", 11L)),
          createDocument(new StringField("name", "ワカメ", Store.YES),
            new SortedDocValuesField("name", new BytesRef("ワカメ")),
            new IntField("age", 9, Store.YES),
            new SortedNumericDocValuesField("age", 9L)),
          createDocument(new StringField("name", "タラオ", Store.YES),
            new SortedDocValuesField("name", new BytesRef("タラオ")),
            new IntField("age", 3, Store.YES),
            new SortedNumericDocValuesField("age", 3L))
        )

        for (reader <- DirectoryReader.open(directory)) {
          val searcher = new IndexSearcher(reader)
          val queryParser = new QueryParser("name", analyzer)
          val query = queryParser.parse("name: カツオ OR name: タラオ")

          val topDocs = searcher.search(query, 10, new Sort(new SortedNumericSortField("age", SortField.Type.INT)))
          topDocs.totalHits should be(2)
          searcher.doc(topDocs.scoreDocs(0).doc).get("name") should be("タラオ")
          searcher.doc(topDocs.scoreDocs(1).doc).get("name") should be("カツオ")

          val topDocs2 = searcher.search(query, 10, new Sort(new SortedSetSortField("name", true)))
          topDocs2.totalHits should be(2)
          searcher.doc(topDocs2.scoreDocs(0).doc).get("name") should be("タラオ")
          searcher.doc(topDocs2.scoreDocs(1).doc).get("name") should be("カツオ")
        }
      })
    }

    it("using DocValues Field, unused SortedDocValuesField, part1") {
      withDirectory(directory => {
        val analyzer = new StandardAnalyzer

        val stringType = new FieldType
        stringType.setDocValuesType(DocValuesType.SORTED)
        stringType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)
        stringType.setStored(true)
        stringType.setTokenized(false)
        stringType.freeze()

        addDocuments(
          directory,
          analyzer,
          createDocument(new Field("name", new BytesRef("カツオ"), stringType), new NumericDocValuesField("age", 11)),
          createDocument(new Field("name", new BytesRef("ワカメ"), stringType), new NumericDocValuesField("age", 9)),
          createDocument(new Field("name", new BytesRef("タラオ"), stringType), new NumericDocValuesField("age", 3))
        )

        for (reader <- DirectoryReader.open(directory)) {
          val searcher = new IndexSearcher(reader)
          val queryParser = new QueryParser("name", analyzer)
          val query = queryParser.parse("name: カツオ OR name: タラオ")

          val topDocs = searcher.search(query, 10, new Sort(new SortField("age", SortField.Type.INT)))
          topDocs.totalHits should be(2)
          searcher.doc(topDocs.scoreDocs(0).doc).getBinaryValue("name").utf8ToString should be("タラオ")
          searcher.doc(topDocs.scoreDocs(1).doc).getBinaryValue("name").utf8ToString should be("カツオ")

          val topDocs2 = searcher.search(query, 10, new Sort(new SortField("name", SortField.Type.STRING_VAL, true)))
          topDocs2.totalHits should be(2)
          searcher.doc(topDocs2.scoreDocs(0).doc).getBinaryValue("name").utf8ToString should be("タラオ")
          searcher.doc(topDocs2.scoreDocs(1).doc).getBinaryValue("name").utf8ToString should be("カツオ")

          val topDocs3 = searcher.search(query, 10, new Sort(new SortField("name", SortField.Type.STRING, true)))
          topDocs3.totalHits should be(2)
          searcher.doc(topDocs3.scoreDocs(0).doc).getBinaryValue("name").utf8ToString should be("タラオ")
          searcher.doc(topDocs3.scoreDocs(1).doc).getBinaryValue("name").utf8ToString should be("カツオ")
        }
      })
    }

    it("using DocValues Field, unused SortedDocValuesField, part2") {
      withDirectory(directory => {
        val analyzer = new StandardAnalyzer

        val stringType = new FieldType
        stringType.setDocValuesType(DocValuesType.SORTED_SET)
        stringType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)
        stringType.setStored(true)
        stringType.setTokenized(false)
        stringType.freeze()

        addDocuments(
          directory,
          analyzer,
          createDocument(new Field("name", new BytesRef("カツオ"), stringType), new NumericDocValuesField("age", 11)),
          createDocument(new Field("name", new BytesRef("ワカメ"), stringType), new NumericDocValuesField("age", 9)),
          createDocument(new Field("name", new BytesRef("タラオ"), stringType), new NumericDocValuesField("age", 3))
        )

        for (reader <- DirectoryReader.open(directory)) {
          val searcher = new IndexSearcher(reader)
          val queryParser = new QueryParser("name", analyzer)
          val query = queryParser.parse("name: カツオ OR name: タラオ")

          val topDocs = searcher.search(query, 10, new Sort(new SortedNumericSortField("age", SortField.Type.INT)))
          topDocs.totalHits should be(2)
          searcher.doc(topDocs.scoreDocs(0).doc).getBinaryValue("name").utf8ToString should be("タラオ")
          searcher.doc(topDocs.scoreDocs(1).doc).getBinaryValue("name").utf8ToString should be("カツオ")

          val topDocs2 = searcher.search(query, 10, new Sort(new SortedSetSortField("name", true)))
          topDocs2.totalHits should be(2)
          searcher.doc(topDocs2.scoreDocs(0).doc).getBinaryValue("name").utf8ToString should be("タラオ")
          searcher.doc(topDocs2.scoreDocs(1).doc).getBinaryValue("name").utf8ToString should be("カツオ")
        }
      })
    }
  }

  protected def createDocument(fields: Field*): Document = {
    val document = new Document
    fields.foreach(document.add)
    document
  }

  protected def addDocuments(directory: Directory, analyzer: Analyzer, documents: Document*): Unit =
    for (writer <- new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
      documents.foreach(writer.addDocument)
      writer.commit()
    }

  protected def withDirectory(f: Directory => Unit): Unit =
    for (directory <- new RAMDirectory) {
      f(directory)
    }
}

object LuceneDocValuesSpec {

  implicit class AutoCloseableWrapper[A <: AutoCloseable](val underying: A) extends AnyVal {
    def foreach(f: A => Unit): Unit = {
      try {
        f(underying)
      } finally {
        underying.close()
      }
    }
  }

}
