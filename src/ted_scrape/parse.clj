(ns ted-scrape.parse
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hickory.select :as hs]
            [ted-scrape.utils :refer [decode-html]]))

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


(defn collect-matching-grandparent-nodes
  "walk the hickory tree
  when a node matches the predicate
  collect its grandparent in a vector"
  [hickory-tree predicate]
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
    (walk hickory-tree nil nil)))

(defn span-with-single-literal-string-content? [node]
  (and (= (:tag node) :span)
       (let [content (:content node)]
         (and (= 1 (count content))
              (string? (first content))))))

(defn collect-matching-nodes
  "walk the hickory tree
  when a node matches the predicate
  collect it in a vector"
  [hickory-tree predicate]
  (letfn [(walk [node]
            (cond
              (map? node)
              (let [matches (if (predicate node) [node] [])]
                (into matches (mapcat walk (:content node))))
              (sequential? node)
              (mapcat walk node)
              :else []))]
    (walk hickory-tree)))

(defn get-span-content [node]
  (first (:content node)))

(defn clean-sentence-fragment [s]
  (-> s
      (str/replace #"\n" " ")
      (str/trim)))

(defn fragments->paragraph [coll]
  (let [timestamp (first coll)
        fragments (rest coll)
        paragraph (str/join " " fragments)]
    {:timestamp timestamp :content paragraph}))

(defn parse-title-speaker [hickory-tree]
  (let [selector (hs/child (hs/tag "head") (hs/tag "title"))
        match (first (hs/select selector hickory-tree))
        content (:content match)
        title (first content)
        split1 (str/split title #":")
        speaker (first split1)
        split2 (str/split (second split1) #"\|")
        name (first split2)]
    {:title (str/trim name) :speaker (str/trim speaker)}))

(defn parse-json-ld [hickory-tree]
  "scrapes a json object
  from a script in the header"
  (let [selector (hs/and (hs/tag :script)
                         (hs/attr :type #(= % "application/ld+json")))
        match (first (hs/select selector hickory-tree))
        str-content (when (and match (:content match))
                      (first (:content match)))]
    (when (string? str-content)
      (json/parse-string str-content))))


(defn parse-description [hickory-tree]
  (let [json (parse-json-ld hickory-tree)
        description (json "description")
        decoded (decode-html description)]
    decoded))

(defn node->paragraph [grandfather-node]
  (-> grandfather-node
      (collect-matching-nodes span-with-single-literal-string-content?)
      (->>
        (map get-span-content)
        (map clean-sentence-fragment)
        (fragments->paragraph))))

(defn parse-ted-talk [input]
  (try
    (cond
      ;; If input is a string, assume it's a path to an EDN file
      (string? input)
      (let [hickory-tree (edn/read-string (slurp (io/file input)))]
        (parse-ted-talk hickory-tree)) ;; recursive call with parsed tree

      ;; If input is already a Hickory tree (map), proceed as usual
      (map? input)
      (let [title-speaker (parse-title-speaker input)
            description (parse-description input)
            transcript-grandparent-nodes (collect-matching-grandparent-nodes input button-with-play-icons?)
            transcript-paragraphs (mapv node->paragraph transcript-grandparent-nodes)
            valid-transcript-paragraphs (filterv #(seq (:content %)) transcript-paragraphs)]
        {:ok? true
         :value {:title       (:title title-speaker)
                 :speaker     (:speaker title-speaker)
                 :description description
                 :transcript  valid-transcript-paragraphs}})

      :else
      {:ok? false
       :error (str "Unsupported input type: " (type input))})

    (catch Exception e
      {:ok? false
       :error (.getMessage e)})))

(defn parse->file [input output-file]
  (let [parse-result (parse-ted-talk input)]
    (if-not (:ok? parse-result)
      parse-result
      (do
        (spit output-file (:value parse-result))
        {:ok? true :value true}))))



(comment
  (parse-ted-talk "data/sample-page.edn")
  (p)
  (parse-ted-talk 12)
  (parse->file "data/sample-page.edn" "data/sample-ted-talk.edn")
  nil)





