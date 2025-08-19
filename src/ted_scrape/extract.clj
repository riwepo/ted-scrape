(ns ted-scrape.extract
  (:require [clojure.java.shell :refer [sh]]
            [hickory.core :as hickory]))


(defn extract-tree [html]
  (-> html
      hickory/parse
      hickory/as-hickory))


;(def html-entities
;  {"&apos;" "'"
;   "&quot;" "\""
;   "&amp;"  "&"
;   "&lt;"   "<"
;   "&gt;"   ">"})

;(defn decode-html [s]
;  (reduce (fn [acc [k v]]
;            (str/replace acc k v))
;          s
;          html-entities))

(defn scrape-html-with-puppeteer-script [url output-file]
  (let [{:keys [exit out err]} (sh "node" "src/scripts/scrapeHtml.js" url output-file)]
    (println "📤 STDOUT:\n" out)
    (when-not (zero? exit)
      (println "⚠️ STDERR:\n" err))
    out))

(defn convert-html-hickory [input-file output-file]
  (->> input-file
       (slurp)
       (extract-tree)
       (spit output-file)))



(comment
    (scrape-html-with-puppeteer-script "https://www.ted.com/talks/nada_majdalani_an_unexpected_plan_for_peace_in_the_middle_east/transcript" "data/full-page-html.txt")
    (convert-html-hickory "data/full-page-html.txt" "data/full-page-hickory.edn")
    (nop))





