(ns ted-scrape.extract
  (:require
    [clojure.java.shell :refer [sh]]
    [hickory.core :as hickory]
    [ted-scrape.utils :refer [decode-html]]))


(defn extract-tree [html]
  (-> html
      hickory/parse
      hickory/as-hickory))

(defn scrape-html-with-puppeteer-script [url output-file]
  (let [{:keys [exit out err]} (sh "node" "src/scripts/scrapeHtml.js" url output-file)]
    (println "📤 STDOUT:\n" out)
    (when-not (zero? exit)
      (println "⚠️ STDERR:\n" err))
    out))

(defn decode-text-nodes [node]
  (cond
    ;; If it's a text node, decode its content
    (and (map? node) (= (:type node) :text))
    (update node :content decode-html)

    ;; If it's a map, walk its values
    (map? node)
    (into {} (map (fn [[k v]] [k (decode-text-nodes v)]) node))

    ;; If it's a vector, walk each element
    (vector? node)
    (mapv decode-text-nodes node)

    ;; Otherwise, leave it unchanged
    :else node))

(defn convert-html-hickory [input-file output-file]
  (->> input-file
       (slurp)
       (extract-tree)
       (decode-text-nodes)
       (spit output-file)))

(comment
  (scrape-html-with-puppeteer-script "https://www.ted.com/talks/nada_majdalani_an_unexpected_plan_for_peace_in_the_middle_east/transcript" "data/full-page-html.txt")
  (convert-html-hickory "data/full-page-html.txt" "data/full-page-hickory.edn")
  (nop))





