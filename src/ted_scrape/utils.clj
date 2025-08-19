(ns ted-scrape.utils
  (:require [clojure.string :as str]))

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