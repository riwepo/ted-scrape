(ns ted-scrape.extract
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :refer [sh]]
    [hickory.core :as hickory]
    [ted-scrape.utils :refer [decode-html]])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))


(defn extract-tree [html]
  (-> html
      hickory/parse
      hickory/as-hickory))

(defn scrape-html-with-puppeteer-script [url]
  "returns a map
  exit is 0 for success, 1 for fail
  out is STDOUT which contains the scraped HTML
  err is STDERR"
  (let [result (sh "node" "resources/scripts/scrapeHtml.js" url)]
    ;(println result)
    result))

(defn run-node-script [resource-path url]
  (let [resource-url (io/resource resource-path)]
    (cond
      (nil? resource-url)
      {:exit 1 :out "" :err (str "Resource not found: " resource-path)}

      (.startsWith (.toString resource-url) "file:")
      ;; Run directly from file system
      (let [file (io/file resource-url)]
        (sh "node" (.getAbsolutePath file) url))

      (.startsWith (.toString resource-url) "jar:")
      ;; Extract to temp file
      (let [temp-file (Files/createTempFile "scrapeHtml" ".js" (make-array FileAttribute 0))
            temp-path (.toFile temp-file)]
        (with-open [in (io/input-stream resource-url)
                    out (io/output-stream temp-path)]
          (io/copy in out))
        (let [result (sh "node" (.getAbsolutePath temp-path) url)]
          (.delete temp-path)
          result)))))




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

(defn html->hickory [html]
  (->> html
       (extract-tree)
       (decode-text-nodes)))

(defn html-file->hickory-file [input-file output-file]
  (-> input-file
      (slurp)
      (html->hickory)
      (spit output-file)))

(comment
  nil)





