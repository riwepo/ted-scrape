(ns ted-scrape.core
  (:require [clj-http.client :as http]
            [ted-scrape.extract :refer [scrape-html-with-puppeteer-script html->hickory]]))

(defn reachable-url?
  "Returns true if the URL responds with a 2xx or 3xx status code."
  [^String url]
  (try
    (let [resp (http/head url {:throw-exceptions false})]
      (<= 200 (:status resp) 399))
    (catch Exception _ false)))


(defn run [{:keys [url output-dir]}]
  (println "running program with these args" url output-dir)
  (if
    (not (reachable-url? url))
    {:status 1 :error "url not reachable"}
    (let [{:keys [exit out] :as result} (scrape-html-with-puppeteer-script url)]
      ;(println exit result)
      (if (not (= 0 exit))
        result
        (do
          (println "hickory")
          (html->hickory out "data/full-page-hickory.edn")
          {:exit 0 :out "finished"})))))                    ; extra steps here




