(ns ted-scrape.core
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.shell :refer [sh]]
            [hickory.core :as hickory]))

(defn extract-tree [html]
  (-> html
      hickory/parse
      hickory/as-hickory))


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
;    (convert-transcript-hickory "transcript-html.txt" "transcript-hickory.edn"))


;(def button-markup
;  {:type  :element,
;   :attrs {:type     "button", :class "group flex items-center justify-between rounded-full bg-gray-50 px-3 py-2 w-20",
;           :tabindex "0"},
;   :tag   :button,
;   :content
;   [{:type  :element,
;     :attrs {:class "relative flex h-4 w-4 items-center justify-center text-red-500"},
;     :tag   :div,
;     :content
;     [{:type  :element,
;       :attrs {:aria-hidden "true", :class "icon-play-filled absolute size-full opacity-0 group-hover:opacity-100"},
;       :tag   :i, :content nil}
;      {:type  :element,
;       :attrs {:aria-hidden "true", :class "icon-play absolute size-full"},
;       :tag   :i, :content nil}]}
;    {:type    :element,
;     :attrs   {:class "text-textPrimary-onLight font-normal text-tui-sm leading-tui-md tracking-tui-tight lg:leading-tui-lg",
;               :dir   "ltr"},
;     :tag     :span,
;     :content ["00:04"]}]})

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


(defn collect-paragraphs
  [tree predicate]
  (letfn [(walk [node parent grandparent]
            (cond
              (map? node)
              (let [matches (if (predicate node)
                              [grandparent]
                              [])]
                (into matches
                      (mapcat #(walk % node parent) (:content node))))

              (sequential? node)
              (mapcat #(walk % parent grandparent) node)

              :else []))]
    (walk tree nil nil)))

(defn span-with-single-content? [node]
  (and (= (:tag node) :span)
       (let [content (:content node)
             count (count content)]
         (and (= 1 count)))))



(defn collect-matching-nodes
  [tree predicate]
  (letfn [(walk [node]
            (cond
              (map? node)
              (let [matches (if (predicate node) [node] [])]
                (into matches (mapcat walk (:content node))))

              (sequential? node)
              (mapcat walk node)

              :else []))]
    (walk tree)))

(defn get-span-content [node]
  (first (:content node)))

(defn clean-sentence_fragment [s]
  (-> s
      (str/replace #"\n" " ")
      (str/trim)))





(defn richo []
  (let [raw-str (slurp "data/transcript-hickory.edn")
        page-markup (edn/read-string raw-str)
        paragraph-collection (collect-paragraphs page-markup button-with-play-icons?)]
    (println (count paragraph-collection))
    (spit "data/paragraph-collection.edn" paragraph-collection)))

(def test-span {:tag :span
                :content ["hello"]})

(defn fred []
  (span-with-single-content? test-span))

(defn richo2 []
  (let [raw-str (slurp "data/first-paragraph.edn")
        paragraph-markup (edn/read-string raw-str)
        sentence-node-collection (collect-matching-nodes paragraph-markup span-with-single-content?)
        sentence-collection (mapv (comp clean-sentence_fragment get-span-content) sentence-node-collection)
        sentence (str/join " " sentence-collection)]
    (spit "data/finished-result.txt" sentence)))



(comment
  (richo)
  (richo2)
  (fred)
  (nop))





