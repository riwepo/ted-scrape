(ns ted-scrape.utils
  (:require [clojure.string :as str])
  (:import (java.io File)))

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

(defn bind [result f]
  "helper function for monadic chaining"
  (if (:ok? result)
    (f (:value result))
    result))



