package org.littlewings.solr

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Properties

import org.apache.commons.io.FileUtils
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.core.{CoreContainer, CoreDescriptor, NodeConfig, SolrResourceLoader}
import org.scalatest.{FunSpec, Matchers}

class EmbeddedSolrServerSpec extends FunSpec with Matchers {
  describe("Embedded Solr Server Spec") {
    ignore("spec solr-home") {
      val coreContainer = new CoreContainer("src/test/resources/embedded-solr-home")
      coreContainer.load()
      val solrServer = new EmbeddedSolrServer(coreContainer, "mycore")

      val doc1 = new SolrInputDocument
      doc1.addField("id", "100")
      doc1.addField("text_txt_ja", "東京都へ遊びに行く")
      solrServer.add(doc1)

      val doc2 = new SolrInputDocument
      doc2.addField("id", "200")
      doc2.addField("text_txt_ja", "すもももももももものうち")
      solrServer.add(doc2)

      solrServer.commit()

      val solrQuery1 = new SolrQuery
      solrQuery1.setQuery("text_txt_ja:東京へ行こう")

      val response1 = solrServer.query(solrQuery1)

      val docList1 = response1.getResults
      docList1 should have size (1)
      docList1.get(0).get("text_txt_ja") should be("東京都へ遊びに行く")

      val solrQuery2 = new SolrQuery
      solrQuery2.setQuery("text_txt_ja:すもも")

      val response2 = solrServer.query(solrQuery2)

      val docList2 = response2.getResults
      docList2 should have size (1)
      docList2.get(0).get("text_txt_ja") should be("すもももももももものうち")

      solrServer.close()
    }

    it("spec core") {
      val solrResourceLoader = new SolrResourceLoader(Paths.get("."))

      val nodeConfig = new NodeConfig.NodeConfigBuilder(null, solrResourceLoader).build

      val coreContainer = new CoreContainer(nodeConfig)
      coreContainer.load()

      val temporaryDataDir = createTemporaryDir("embedded-solr")
      val properties = new Properties
      properties.put(CoreDescriptor.CORE_DATADIR, temporaryDataDir)
      properties.put(CoreDescriptor.CORE_CONFIG,
        toAbsolutePath("src/test/resources/embedded-solr-core/conf/solrconfig.xml"))

      val coreDescriptor =
        new CoreDescriptor(coreContainer,
          "embedded-solr",
          "src/test/resources/embedded-solr-core",
          properties)

      val solrCore = coreContainer.create(coreDescriptor)
      val solrServer = new EmbeddedSolrServer(solrCore)

      val doc1 = new SolrInputDocument
      doc1.addField("id", "100")
      doc1.addField("text_txt_ja", "東京都へ遊びに行く")
      solrServer.add(doc1)

      val doc2 = new SolrInputDocument
      doc2.addField("id", "200")
      doc2.addField("text_txt_ja", "すもももももももものうち")
      solrServer.add(doc2)

      solrServer.commit()

      val solrQuery1 = new SolrQuery
      solrQuery1.setQuery("text_txt_ja:東京へ行こう")

      val response1 = solrServer.query(solrQuery1)

      val docList1 = response1.getResults
      docList1 should have size (1)
      docList1.get(0).get("text_txt_ja") should be("東京都へ遊びに行く")

      val solrQuery2 = new SolrQuery
      solrQuery2.setQuery("text_txt_ja:すもも")

      val response2 = solrServer.query(solrQuery2)

      val docList2 = response2.getResults
      docList2 should have size (1)
      docList2.get(0).get("text_txt_ja") should be("すもももももももものうち")

      solrServer.close()

      FileUtils.deleteDirectory(new File(temporaryDataDir))
    }
  }

  protected def createTemporaryDir(dirName: String): String =
    Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), dirName).toString

  protected def toAbsolutePath(path: String): String =
    new File(path).getAbsolutePath
}
