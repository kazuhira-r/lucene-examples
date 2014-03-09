(set! *warn-on-reflection* true)

(require '[leiningen.exec :as exec])

(exec/deps '[[clj-soup/clojure-soup "0.1.1"]
             [org.apache.lucene/lucene-kuromoji "3.6.2"]
             [incanter "1.5.4"]])

(ns lucene.clojure.word.count
    (import (java.io StringReader)
            (org.apache.lucene.analysis Analyzer TokenStream)
            (org.apache.lucene.analysis.ja JapaneseAnalyzer)
            (org.apache.lucene.analysis.ja.tokenattributes BaseFormAttribute InflectionAttribute PartOfSpeechAttribute ReadingAttribute)
            (org.apache.lucene.analysis.tokenattributes CharTermAttribute)
            (org.apache.lucene.util Version))
    (:require [jsoup.soup :as js]
              [incanter.core :as c]
              [incanter.stats :as s]
              [incanter.charts :as ch]))

;; Luceneのバージョン
(def ^Version lucene-version (Version/LUCENE_CURRENT))

;; 与えられた文字列を、形態素解析し単語および属性のマップとして
;; ベクタに含めて返却する
(defn morphological-analysis [^String sentence]
  (let [^Analyzer analyzer (JapaneseAnalyzer. lucene-version)]
    (with-open [^TokenStream token-stream (. analyzer tokenStream
                                             ""
                                             (StringReader. sentence))]

      (let [^CharTermAttribute char-term (. token-stream addAttribute CharTermAttribute)
            ^BaseFormAttribute base-form (. token-stream addAttribute BaseFormAttribute)
            ^InflectionAttribute inflection (. token-stream addAttribute InflectionAttribute)
            ^PartOfSpeechAttribute part-of-speech (. token-stream addAttribute PartOfSpeechAttribute)
            ^ReadingAttribute reading (. token-stream addAttribute ReadingAttribute)]

        (letfn [(create-attributes []
                  {:token (. char-term toString)
                   :reading (. reading getReading)
                   :part-of-speech (. part-of-speech getPartOfSpeech)
                   :base (. base-form getBaseForm)
                   :inflection-type (. inflection getInflectionType)
                   :inflection-form (. inflection getInflectionForm)})]
          (. token-stream reset)

          (try
            (loop [tokenized-seq []]
              (if (. token-stream incrementToken)
                (recur (conj tokenized-seq (create-attributes)))
                tokenized-seq))
            (finally (. token-stream end))))))))

;; morphological-analysis関数で得られたベクタの中から
;; 名詞のみに絞り込む
(defn select-nominal [tokenized-seq]
  (filter #(re-find #"名詞" (% :part-of-speech)) tokenized-seq))

;; morphological-analysis関数で得られたベクタの中から
;; 単語のみを抽出する
(defn token-only [tokenized-seq]
  (map #(% :token) tokenized-seq))

;; 文字列のベクタを、単語と出現回数のマップに変換する
(defn word-count [token-seq]
  (reduce (fn [words word]
            (assoc words word (inc (get words word 0))))
          {}
          token-seq))

;; word-count関数の結果を、出現回数の降順にソートする
(defn sort-desc-words [word-counted-map]
  (reverse (sort-by second word-counted-map)))


(comment
(let [wc (->> (morphological-analysis (str "ClojureとLucene-Kuromojiを使って、"
                                  "テキストを形態素解析してワードカウントを行い、"
                                  "Clojureによるテキストマイニングにチャレンジしようと思います"))
              (select-nominal)
              (token-only)
              (word-count)
              (sort-desc-words)
              (take 5))]
  (c/view (ch/bar-chart (keys wc) (vals wc))))
)

(let [^String text (js/$ (js/get! "http://www.aozora.gr.jp/cards/000148/files/752_14964.html")
                         "div.main_text"
                         (js/text)
                         (clojure.string/join ""))]
  (let [wc (->> (morphological-analysis text)
                (select-nominal)
                (token-only)
                (word-count)
                (sort-desc-words))
        top10 (take 10 wc)
        top100 (take 100 wc)]
    (c/view (ch/bar-chart (keys top10)
                          (vals top10)
                          :title "頻出単語 上位10位と出現数"
                          :x-label "単語"
                          :y-label "出現数"))
    (c/view (ch/bar-chart (keys top100)
                          (vals top100)
                          :title "頻出単語 上位100位と出現数"
                          :x-label "単語"
                          :y-label "出現数"))))

;; 終了待ち
(println "Enterをすると終了します")
(read-line)
