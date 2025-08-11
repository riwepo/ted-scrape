(ns ted-scrape.core
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clj-http.client :as http]
            [hickory.core :as hickory]))


(defn fetch-transcript-html [slug]
  (let [url (str "https://www.ted.com/talks/" slug "/transcript")]
    (:body (http/get url {:headers {"User-Agent" "ted-scrape-bot"}}))))

(defn save-transcript-html [slug]
  (->> slug
       (fetch-transcript-html)
       (spit "transcript.txt")))

(defn extract-tree [html]
  (-> html
      hickory/parse
      hickory/as-hickory))

(defn format-node [node indent]
  (let [pad (apply str (repeat indent "  "))]               ; 2 spaces per level
    (cond
      ;; Raw string content
      (string? node)
      (str pad node)

      ;; Text node
      (= (:type node) :text)
      (str pad (:content node))

      ;; Element node
      (= (:type node) :element)
      (let [tag (name (:tag node))
            children (:content node)
            inner (if (seq children)
                    (->> children
                         (map #(format-node % (inc indent)))
                         (str/join "\n"))
                    nil)]
        (if inner
          (str pad "<" tag ">\n"
               inner "\n"
               pad "</" tag ">")
          (str pad "<" tag "></" tag ">")))

      ;; Document node
      (= (:type node) :document)
      (->> (:content node)
           (map #(format-node % indent))
           (str/join "\n"))

      :else
      "")))

(defn slug->file [slug]
  (let [document-node (-> slug
                          fetch-transcript-html
                          extract-tree)
        html-node (first (filter #(= (select-keys % [:type :tag]) {:type :element :tag :html}) (:content document-node)))
        body-node (first (filter #(= (select-keys % [:type :tag]) {:type :element :tag :body}) (:content html-node)))
        formatted (format-node body-node 2)]
    ;(println doc-node)))
    (spit "output.txt" formatted)))



(defn extract-linked-data-script [hickory-tree]
  (let [match? (fn [node]
                 (and (map? node)
                      (= (:type node) :element)
                      (= (:tag node) :script)
                      (= (get-in node [:attrs :type]) "application/ld+json")
                      (= (get-in node [:attrs :data-next-head]) "")))]
    (some #(when (match? %) %) (tree-seq coll? seq hickory-tree))))

(def html-entities
  {"&apos;" "'"
   "&quot;" "\""
   "&amp;"  "&"
   "&lt;"   "<"
   "&gt;"   ">"})

(defn decode-html [s]
  (reduce (fn [acc [k v]]
            (str/replace acc k v))
          s
          html-entities))





(defn richo []
  (-> "transcript.txt"
      slurp
      extract-tree
      extract-linked-data-script
      :content
      first
      (json/parse-string true)
      :transcript
      decode-html
      (->>
        (spit "scripts.txt"))))



(def tree
  {:type    :document,
   :content [{:type    :element,
              :attrs   nil,
              :tag     :html,
              :content [{:type :element, :attrs nil, :tag :head, :content nil}
                        {:type    :element,
                         :attrs   nil,
                         :tag     :body,
                         :content [{:type :element, :attrs nil, :tag :p, :content ["Hello world"]}]}]}]})

(def p {:type :element, :attrs nil, :tag :p, :content ["Hello world"]})
(comment
  (richo)
  (decode-html "we didn&apos;t understand")
  (save-transcript-html "eric_schmidt_the_ai_revolution_is_underhyped")
  (slug->file "eric_schmidt_the_ai_revolution_is_underhyped")
  (format-node (extract-tree "<p>Hello <strong>world</strong>.</p>") 0)
  (extract-tree "<p>Hello world</p>")
  (format-node (extract-tree "<p>Hello world</p>") 2)
  (format-node tree) 2)


