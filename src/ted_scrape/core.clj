(ns ted-scrape.core
  (:require [ted-scrape.extract :refer [scrape->hickory]]
            [ted-scrape.parse :refer [parse-ted-talk]]
            [ted-scrape.odt-writer :refer [save-ted-talk]]
            [ted-scrape.utils :refer [bind]]))


(defn process-ted-talk [output-dir url]
  (-> (scrape->hickory url)
      (bind parse-ted-talk)
      (bind (partial save-ted-talk output-dir))))

(comment
  (bind (scrape->hickory "https://www.ted.com/talks/eric_schmidt_the_ai_revolution_is_underhyped/transcript") parse-ted-talk)
  (bind {:ok? true :value "data/sample-page.edn"} parse-ted-talk)
  (process-ted-talk
    "data"
    "https://www.ted.com/talks/eric_schmidt_the_ai_revolution_is_underhyped/transcript")
  nil)






