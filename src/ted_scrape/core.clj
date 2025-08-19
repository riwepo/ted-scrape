(ns ted-scrape.core
  (:require [clj-http.client :as http]))

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
      {:ok? false :message "url not reachable"}
      {:ok? true :message "finished"}))


