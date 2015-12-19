package org.littlewings.lucene.wildcardquery

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.{Document, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.queryparser.classic.{ParseException, QueryParser}
import org.apache.lucene.search.{IndexSearcher, Sort, WildcardQuery}
import org.apache.lucene.store.RAMDirectory
import org.scalatest.{FunSpec, Matchers}

class WildcardQuerySpec extends FunSpec with Matchers {
  describe("WildcardQuery Spec") {
    it("prefix search, using WildcardQuery") {
      val analyzer = new StandardAnalyzer
      val directory = new RAMDirectory

      val indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))
      val document1 = {
        val d = new Document()
        d.add(new TextField("name", "foo_bar", Store.YES))
        d
      }
      val document2 = {
        val d = new Document()
        d.add(new TextField("name", "bar_foo", Store.YES))
        d
      }

      indexWriter.addDocument(document1)
      indexWriter.addDocument(document2)
      indexWriter.commit()
      indexWriter.close()

      val query = new WildcardQuery(new Term("name", "*foo"))

      val reader = DirectoryReader.open(directory)
      val searcher = new IndexSearcher(reader)

      val topDocs = searcher.search(query, 100, Sort.RELEVANCE)
      val resultDoc = searcher.doc(topDocs.scoreDocs(0).doc)
      resultDoc.get("name") should be("bar_foo")

      directory.close()
    }

    it("prefix search, using QueryParser") {
      val analyzer = new StandardAnalyzer
      val directory = new RAMDirectory

      val indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))
      val document1 = {
        val d = new Document()
        d.add(new TextField("name", "foo_bar", Store.YES))
        d
      }
      val document2 = {
        val d = new Document()
        d.add(new TextField("name", "bar_foo", Store.YES))
        d
      }

      indexWriter.addDocument(document1)
      indexWriter.addDocument(document2)
      indexWriter.commit()
      indexWriter.close()

      val queryParser = new QueryParser("name", analyzer)
      a[ParseException] should be thrownBy queryParser.parse("*_foo")

      directory.close()
    }
  }
}
