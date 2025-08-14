(ns ted-scrape.core
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.shell :refer [sh]]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [hickory.core :as hickory]))


;(defn fetch-transcript-html [slug]
;  (let [url (str "https://www.ted.com/talks/" slug "/transcript")]
;    (:body (http/get url {:headers {"User-Agent" "ted-scrape-bot"}}))))

;(defn save-transcript-html [slug]
;  (->> slug
;       (fetch-transcript-html)
;       (spit "transcript-html.txt")))

(defn extract-tree [html]
  (-> html
      hickory/parse
      hickory/as-hickory))

;(defn save-transcript-hickory [slug]
;  (->> slug
;       (fetch-transcript-html)
;       (extract-tree)
;       (spit "transcript-hickory.txt")))

;(defn format-node [node indent]
;  (let [pad (apply str (repeat indent "  "))]               ; 2 spaces per level
;    (cond
;      ;; Raw string content
;      (string? node)
;      (str pad node)
;
;      ;; Text node
;      (= (:type node) :text)
;      (str pad (:content node))
;
;      ;; Element node
;      (= (:type node) :element)
;      (let [tag (name (:tag node))
;            children (:content node)
;            inner (if (seq children)
;                    (->> children
;                         (map #(format-node % (inc indent)))
;                         (str/join "\n"))
;                    nil)]
;        (if inner
;          (str pad "<" tag ">\n"
;               inner "\n"
;               pad "</" tag ">")
;          (str pad "<" tag "></" tag ">")))
;
;      ;; Document node
;      (= (:type node) :document)
;      (->> (:content node)
;           (map #(format-node % indent))
;           (str/join "\n"))
;
;      :else
;      "")))

;(defn slug->file [slug]
;  (let [document-node (-> slug
;                          fetch-transcript-html
;                          extract-tree)
;        html-node (first (filter #(= (select-keys % [:type :tag]) {:type :element :tag :html}) (:content document-node)))
;        body-node (first (filter #(= (select-keys % [:type :tag]) {:type :element :tag :body}) (:content html-node)))
;        formatted (format-node body-node 2)]
;    ;(println doc-node)))
;    (spit "output.txt" formatted)))

;(defn extract-linked-data-script [hickory-tree]
;  (let [match? (fn [node]
;                 (and (map? node)
;                      (= (:type node) :element)
;                      (= (:tag node) :script)
;                      (= (get-in node [:attrs :type]) "application/ld+json")
;                      (= (get-in node [:attrs :data-next-head]) "")))]
;    (some #(when (match? %) %) (tree-seq coll? seq hickory-tree))))

;(def html-entities
;  {"&apos;" "'"
;   "&quot;" "\""
;   "&amp;"  "&"
;   "&lt;"   "<"
;   "&gt;"   ">"})

;(defn decode-html [s]
;  (reduce (fn [acc [k v]]
;            (str/replace acc k v))
;          s
;          html-entities))

;(defn richo []
;  (-> "transcript-html.txt"
;      slurp
;      extract-tree
;      extract-linked-data-script
;      :content
;      first
;      (json/parse-string true)
;      :transcript
;      decode-html
;      (->>
;        (spit "scripts.txt"))))


(defn scrape-html-with-puppeteer-script [url output-file]
  (let [{:keys [exit out err]} (sh "node" "src/scripts/scrapeHtml.js" url output-file)]
    (println "📤 STDOUT:\n" out)
    (when-not (zero? exit)
      (println "⚠️ STDERR:\n" err))
    out))

(defn convert-transcript-hickory [input-file output-file]
  (->> input-file
       (slurp)
       (extract-tree)
       (spit output-file)))

;(defn richo []
;   (scrape-html-with-puppeteer-script
;     "https://www.ted.com/talks/eric_schmidt_the_ai_revolution_is_underhyped/transcript"
;     "transcript-html.txt")
;    (convert-transcript-hickory "transcript-html.txt" "transcript-hickory.txt"))

;(def tree
;  {:type    :document,
;   :content [{:type    :element,
;              :attrs   nil,
;              :tag     :html,
;              :content [{:type :element, :attrs nil, :tag :head, :content nil}
;                        {:type    :element,
;                         :attrs   nil,
;                         :tag     :body,
;                         :content [{:type :element, :attrs nil, :tag :p, :content ["Hello world"]}]}]}]})

;(def p {:type :element, :attrs nil, :tag :p, :content ["Hello world"]})

(def button-markup
  {:type  :element,
   :attrs {:type     "button", :class "group flex items-center justify-between rounded-full bg-gray-50 px-3 py-2 w-20",
           :tabindex "0"},
   :tag   :button,
   :content
   [{:type  :element,
     :attrs {:class "relative flex h-4 w-4 items-center justify-center text-red-500"},
     :tag   :div,
     :content
     [{:type  :element,
       :attrs {:aria-hidden "true", :class "icon-play-filled absolute size-full opacity-0 group-hover:opacity-100"},
       :tag   :i, :content nil}
      {:type  :element,
       :attrs {:aria-hidden "true", :class "icon-play absolute size-full"},
       :tag   :i, :content nil}]}
    {:type    :element,
     :attrs   {:class "text-textPrimary-onLight font-normal text-tui-sm leading-tui-md tracking-tui-tight lg:leading-tui-lg",
               :dir   "ltr"},
     :tag     :span,
     :content ["00:04"]}]})

(defn button-with-play-icons? [node]
  (and (= (:tag node) :button)
       (let [content (:content node)
             div (first content)
             icons (:content div)]
         (and (= (:tag div) :div)
              (= (count icons) 2)
              (every? #(= (:tag %) :i) icons)
              (some
                #(str/includes?
                   (get-in % [:attrs :class] "") "icon-play-filled") icons)
              (some
                #(str/includes?
                   (get-in % [:attrs :class] "") "icon-play") icons)))))

(defn collect-buttons [atom node]
  (when (map? node)
    (when (button-with-play-icons? node)
      (swap! atom conj node))
    (doseq [child (:content node)]
      (collect-buttons atom child))))

(defn richo []
  (let [matched-buttons (atom [])
        raw-str (slurp "transcript-hickory.txt")
        page-markup (edn/read-string raw-str)]
    (collect-buttons matched-buttons page-markup)
    @matched-buttons))

(comment
  (richo)
  (nop))





