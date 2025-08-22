(ns ted-scrape.core
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [ted-scrape.extract :refer [scrape-html-with-puppeteer-script html->hickory]]
            [ted-scrape.parse :refer [parse-ted-talk]]
            [ted-scrape.odt-writer :refer [write-ted-talk]]
            [clojure.java.io :as io]))

(defn reachable-url?
  "Returns true if the URL responds with a 2xx or 3xx status code."
  [^String url]
  (try
    (let [resp (http/head url {:throw-exceptions false})]
      (<= 200 (:status resp) 399))
    (catch Exception _ false)))


(defn run [{:keys [url output-dir]}]
  (println "running ted-scrape.core.run with args: " url output-dir)
  (if
    (not (reachable-url? url))
    (do
      (println "url not reachable")
      {:status 1 :error "url not reachable"})
    (let [{:keys [exit out] :as result} (scrape-html-with-puppeteer-script url)]
      (println "scrape-html-with-puppeteer-script returned exit status " exit)
      (if-not (= 0 exit)
        result
        (let [hickory-tree (html->hickory out)
              ted-talk (parse-ted-talk hickory-tree)
              title (str/replace (:title ted-talk) #" " "-")
              doc (write-ted-talk ted-talk)]
          (.save doc (io/file output-dir (str title ".odt")))
          {:exit 0})))))




