(ns ted-scrape.extract
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [hickory.core :as hickory]
    [config.core :refer [env]]
    [ted-scrape.utils :refer [decode-html]]))

(defn scrape-html
  "Posts a target URL and selector to the scrape-api and returns the HTML result.
   Returns {:ok? true :value <html>} on success,
           {:ok? false :error <message>} on failure."
  [url]
  (try
    (let [endpoint (:scrape-api-url env)
          selector (:scrape-api-selector env)
          payload {:url url :selector selector}
          headers {"Content-Type" "application/json"}
          response (http/post endpoint
                              {:headers headers
                               :body    (json/generate-string payload)
                               :as      :json})
          html (get-in response [:body :html])]
      (println payload)
      (if html
        {:ok? true :value html}
        {:ok? false :error "Unexpected response"}))
    (catch Exception e
      {:ok? false :error (.getMessage e)})))

(defn extract-tree [html]
  (-> html
      hickory/parse
      hickory/as-hickory))

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

(defn scrape->hickory
  "Scrapes HTML from the scrape-api and parses it into decoded Hickory tree.
   Returns {:ok? true :value <parsed tree>} on success,
           {:ok? false :error <message>} on failure."
  [url]
  (let [scrape-result (scrape-html url)]
    (if-not (:ok? scrape-result)
      scrape-result
      (try
        (let [html (:value scrape-result)
              parsed (->> html
                          (extract-tree)
                          (decode-text-nodes))]
          {:ok? true :value parsed})
        (catch Exception e
          {:ok? false :error (.getMessage e)})))))

(defn scrape->file [url output-file]
  "dev function to save web page to edn file in hickory format"
  (->> (scrape->hickory url)
       (:value)
       (spit output-file)))

(comment
  (scrape->hickory
    "https://www.ted.com/talks/eric_schmidt_the_ai_revolution_is_underhyped/transcript")
  (scrape->file
    "https://www.ted.com/talks/eric_schmidt_the_ai_revolution_is_underhyped/transcript"
    "data/sample-page.edn")
  nil)





