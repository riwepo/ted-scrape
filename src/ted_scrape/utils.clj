(ns ted-scrape.utils
  (:require [clojure.string :as str])
  (:import (java.io File)))

(defn debug-print [val]
  (println "Debugging:" val)
  val)

(defn decode-html [s]
  (let [html-entities {"&apos;" "'"
                       "&quot;" "\""
                       "&amp;"  "&"
                       "&lt;"   "<"
                       "&gt;"   ">"}]
    (reduce (fn [acc [k v]]
              (str/replace acc k v))
            s
            html-entities)))

(defn valid-ted-transcript-url? [url]
  (let [pattern #"^https://www\.ted\.com/talks/([a-z0-9_\-]+)/transcript$"
        match (re-matches pattern url)]
    (boolean match)))

(defn valid-output-dir?
  "Returns true if the path exists, is a directory, and is writable."
  [^String path]
  (let [f (File. path)]
    (and (.exists f)
         (.isDirectory f)
         (.canWrite f))))





