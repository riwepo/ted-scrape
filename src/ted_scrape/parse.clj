(ns ted-scrape.parse
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [hickory.select :as hs]))

;(defn richo []
;   (scrape-html-with-puppeteer-script
;     "https://www.ted.com/talks/eric_schmidt_the_ai_revolution_is_underhyped/transcript"
;     "full-page-html.txt")
;    (convert-transcript-hickory "full-page-html.txt" "transcript-hickory.edn"))


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

(defn span-with-single-literal-string-content? [node]
  (and (= (:tag node) :span)
       (let [content (:content node)]
         (and (= 1 (count content))
              (string? (first content))))))

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

(defn fragments->paragraph [coll]
  (let [timestamp (first coll)
        fragments (rest coll)
        paragraph (str/join " " fragments)]
    {:timestamp timestamp :content paragraph}))

(defn scrape-speaker-title [tree]
  (let [selector (hs/child (hs/tag "head") (hs/tag "title"))
        match (first (hs/select selector tree))
        content (:content match)
        title (first content)
        split1 (str/split title #":")
        speaker (first split1)
        split2 (str/split (second split1) #"\|")
        name (first split2)]
    {:speaker (str/trim speaker) :title (str/trim name)}))

;(defn scrape-description [tree]
;  (let [selector (hs/and (hs/tag :script) (hs/attr :type "application/ld+json"))
;        match (first (hs/select selector tree))]
;    match))

;(defn scrape-json-ld [tree]
;  (let [selector (hs/and (hs/tag :script) (hs/attr :type #(= % "application/ld+json")))
;        match (first (hs/select selector tree))
;        str-content (first (:content match))
;        content (json/parse-string str-content)]
;    content))

(defn scrape-json-ld [tree]
  (let [selector (hs/and (hs/tag :script)
                         (hs/attr :type #(= % "application/ld+json")))
        match (first (hs/select selector tree))
        str-content (when (and match (:content match))
                      (first (:content match)))]
    (when (string? str-content)
      (json/parse-string str-content))))




(defn scrape-description [tree]
  (let [json (scrape-json-ld tree)
        description (json "description")]
    description))


(defn richo6 []
  (let [raw-str (slurp "data/transcript-hickory.edn")
        full-page-hiccup (edn/read-string raw-str)
        talk-speaker-title (scrape-speaker-title full-page-hiccup)]
    (println full-page-hiccup)
    (println talk-speaker-title)))

(defn richo7 []
  (let [raw-str (slurp "data/transcript-hickory.edn")
        full-page-hiccup (edn/read-string raw-str)
        speaker-title (scrape-speaker-title full-page-hiccup)]
    speaker-title))

(defn richo8 []
  (let [raw-str (slurp "data/transcript-hickory.edn")
        full-page-hiccup (edn/read-string raw-str)
        description (scrape-description full-page-hiccup)]
    description))


(defn richo []
  (let [raw-str (slurp "data/transcript-hickory.edn")
        page-markup (edn/read-string raw-str)
        paragraph-collection (collect-paragraphs page-markup button-with-play-icons?)]
    (println (count paragraph-collection))
    (spit "data/paragraph-collection.edn" paragraph-collection)))

(defn richo2 []
  (let [raw-str (slurp "data/first-paragraph.edn")
        paragraph-markup (edn/read-string raw-str)
        sentence-node-collection (collect-matching-nodes paragraph-markup span-with-single-literal-string-content?)
        sentence-collection (mapv (comp clean-sentence_fragment get-span-content) sentence-node-collection)
        sentence (str/join " " sentence-collection)]
    (spit "data/finished-result.txt" sentence)))

(defn tree->paragraph [tree]
  (-> tree
      (collect-matching-nodes span-with-single-literal-string-content?)
      (->>
        (map get-span-content)
        (map clean-sentence_fragment)
        (fragments->paragraph))))

(defn richo3 []
  (let [raw-str (slurp "data/test-paragraph.edn")
        paragraph-markup (edn/read-string raw-str)
        paragraph (tree->paragraph paragraph-markup)]
    (spit "data/finished-result.txt" paragraph)))

(defn richo4 []
  (let [raw-str (slurp "data/paragraph-collection.edn")
        tree-collection (edn/read-string raw-str)
        paragraph-collection (mapv tree->paragraph tree-collection)
        valid-paragraphs (filterv #(seq (:content %)) paragraph-collection)]
    ;transcript (str/join "\n" paragraph-collection)]
    (spit "data/finished-result.txt" valid-paragraphs)))

(defn richo5 []
  (let [raw-str (slurp "data/paragraph-collection.edn")
        tree-collection (edn/read-string raw-str)
        last (nth tree-collection 59)]
    (spit "data/test-paragraph.edn" last)))

(comment
  (richo6)
  (richo7)
  (richo8)
  (nop))





