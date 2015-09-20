package org.littlewings.lucene

import org.apache.lucene.search.spell.{NGramDistance, JaroWinklerDistance, LuceneLevenshteinDistance, LevensteinDistance}
import org.scalatest.FunSpec
import org.scalatest.Matchers._

class LuceneLevensteinDistanceSpec extends FunSpec {
  describe("Lucene Levenstein Distance Spec") {
    it("using LevensteinDistance") {
      val levensteinDistance = new LevensteinDistance

      levensteinDistance.getDistance("java", "java") should be (1)
      levensteinDistance.getDistance("java", "javo") should be (0.75)
      levensteinDistance.getDistance("java", "jajo") should be (0.5)
      levensteinDistance.getDistance("java", "jojo") should be (0.25)
      levensteinDistance.getDistance("java", "jovo") should be (0.5)

      levensteinDistance.getDistance("磯野カツオ", "磯野カツオ") should be (1)
      levensteinDistance.getDistance("磯野カツオ", "磯野カツヲ") should be <= (0.8F)
      levensteinDistance.getDistance("磯野カツオ", "磯野カケヲ") should be <= (0.6F)
      levensteinDistance.getDistance("磯野カツオ", "海野カケヲ") should be <= (0.4F)

      levensteinDistance.getDistance("Saturday", "Sunday") should be (0.625F)
      levensteinDistance.getDistance("Solr", "Solar") should be <= (0.8F)
      levensteinDistance.getDistance("Apache Solr", "Apache Lucene") should be <= (0.54F)
      levensteinDistance.getDistance("Apache Hadoop", "Apache Hadoop") should be (1)
      levensteinDistance.getDistance("ABC", "XYZ") should be (0)
    }

    it("using LuceneLevenshteinDistance") {
      val levensteinDistance = new LuceneLevenshteinDistance

      levensteinDistance.getDistance("java", "java") should be (1)
      levensteinDistance.getDistance("java", "javo") should be (0.75F)
      levensteinDistance.getDistance("java", "jajo") should be (0.5F)
      levensteinDistance.getDistance("java", "jojo") should be (0.25F)
      levensteinDistance.getDistance("java", "jovo") should be (0.5F)

      levensteinDistance.getDistance("磯野カツオ", "磯野カツオ") should be (1)
      levensteinDistance.getDistance("磯野カツオ", "磯野カツヲ") should be <= (0.8F)
      levensteinDistance.getDistance("磯野カツオ", "磯野カケヲ") should be <= (0.6F)
      levensteinDistance.getDistance("磯野カツオ", "海野カケヲ") should be <= (0.4F)

      levensteinDistance.getDistance("Saturday", "Sunday") should be (0.5F)
      levensteinDistance.getDistance("Solr", "Solar") should be <= (0.8F)
      levensteinDistance.getDistance("Apache Solr", "Apache Lucene") should be <= (0.54F)
      levensteinDistance.getDistance("Apache Hadoop", "Apache Hadoop") should be (1)
      levensteinDistance.getDistance("ABC", "XYZ") should be (0)
    }

    it("using JaroWinklerDistance") {
      val levensteinDistance = new JaroWinklerDistance

      levensteinDistance.getDistance("java", "java") should be (1)
      levensteinDistance.getDistance("java", "javo") should be <= (0.89F)
      levensteinDistance.getDistance("java", "jajo") should be <= (0.67F)
      levensteinDistance.getDistance("java", "jojo") should be (0.5F)
      levensteinDistance.getDistance("java", "jovo") should be <= (0.67F)

      levensteinDistance.getDistance("磯野カツオ", "磯野カツオ") should be (1)
      levensteinDistance.getDistance("磯野カツオ", "磯野カツヲ") should be <= (0.92F)
      levensteinDistance.getDistance("磯野カツオ", "磯野カケヲ") should be <= (0.82F)
      levensteinDistance.getDistance("磯野カツオ", "海野カケヲ") should be <= (0.6F)

      levensteinDistance.getDistance("Saturday", "Sunday") should be (0.7775F)
      levensteinDistance.getDistance("Solr", "Solar") should be <= (0.96F)
      levensteinDistance.getDistance("Apache Solr", "Apache Lucene") should be <= (0.88F)
      levensteinDistance.getDistance("Apache Hadoop", "Apache Hadoop") should be (1)
      levensteinDistance.getDistance("ABC", "XYZ") should be (0)
    }

    it("using JaroWinklerDistance with threshold") {
      val levensteinDistance = new JaroWinklerDistance
      levensteinDistance.setThreshold(0.5F)

      levensteinDistance.getDistance("java", "java") should be (1)
      levensteinDistance.getDistance("java", "javo") should be <= (0.89F)
      levensteinDistance.getDistance("java", "jajo") should be <= (0.74F)
      levensteinDistance.getDistance("java", "jojo") should be (0.55F)
      levensteinDistance.getDistance("java", "jovo") should be <= (0.71F)

      levensteinDistance.getDistance("磯野カツオ", "磯野カツオ") should be (1)
      levensteinDistance.getDistance("磯野カツオ", "磯野カツヲ") should be <= (0.92F)
      levensteinDistance.getDistance("磯野カツオ", "磯野カケヲ") should be <= (0.82F)
      levensteinDistance.getDistance("磯野カツオ", "海野カケヲ") should be <= (0.6F)

      levensteinDistance.getDistance("Saturday", "Sunday") should be (0.7775F)
      levensteinDistance.getDistance("Solr", "Solar") should be <= (0.96F)
      levensteinDistance.getDistance("Apache Solr", "Apache Lucene") should be <= (0.88F)
      levensteinDistance.getDistance("Apache Hadoop", "Apache Hadoop") should be (1)
      levensteinDistance.getDistance("ABC", "XYZ") should be (0)
    }

    it("using NGramDistance") {
      val levensteinDistance = new NGramDistance

      levensteinDistance.getDistance("java", "java") should be (1)
      levensteinDistance.getDistance("java", "javo") should be (0.875F)
      levensteinDistance.getDistance("java", "jajo") should be (0.625F)
      levensteinDistance.getDistance("java", "jojo") should be (0.375F)
      levensteinDistance.getDistance("java", "jovo") should be (0.625F)

      levensteinDistance.getDistance("磯野カツオ", "磯野カツオ") should be (1)
      levensteinDistance.getDistance("磯野カツオ", "磯野カツヲ") should be (0.9F)
      levensteinDistance.getDistance("磯野カツオ", "磯野カケヲ") should be (0.7F)
      levensteinDistance.getDistance("磯野カツオ", "海野カケヲ") should be <= (0.4F)

      levensteinDistance.getDistance("Saturday", "Sunday") should be (0.5625F)
      levensteinDistance.getDistance("Solr", "Solar") should be (0.7F)
      levensteinDistance.getDistance("Apache Solr", "Apache Lucene") should be <= (0.577F)
      levensteinDistance.getDistance("Apache Hadoop", "Apache Hadoop") should be (1)
      levensteinDistance.getDistance("ABC", "XYZ") should be (0)
    }

    it("using NGramDistance Tri-Gram") {
      val levensteinDistance = new NGramDistance(3)

      levensteinDistance.getDistance("java", "java") should be (1)
      levensteinDistance.getDistance("java", "javo") should be <= (0.92F)
      levensteinDistance.getDistance("java", "jajo") should be (0.75F)
      levensteinDistance.getDistance("java", "jojo") should be <= (0.46F)
      levensteinDistance.getDistance("java", "jovo") should be (0.625F)

      levensteinDistance.getDistance("磯野カツオ", "磯野カツオ") should be (1)
      levensteinDistance.getDistance("磯野カツオ", "磯野カツヲ") should be <= (0.94F)
      levensteinDistance.getDistance("磯野カツオ", "磯野カケヲ") should be (0.8F)
      levensteinDistance.getDistance("磯野カツオ", "海野カケヲ") should be <= (0.44F)

      levensteinDistance.getDistance("Saturday", "Sunday") should be <= (0.53F)
      levensteinDistance.getDistance("Solr", "Solar") should be <= (0.74F)
      levensteinDistance.getDistance("Apache Solr", "Apache Lucene") should be <= (0.62F)
      levensteinDistance.getDistance("Apache Hadoop", "Apache Hadoop") should be (1)
      levensteinDistance.getDistance("ABC", "XYZ") should be (0)
    }
  }
}
