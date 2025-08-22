(ns ted-scrape.odt-writer
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [org.odftoolkit.odfdom.doc OdfTextDocument]
           [org.odftoolkit.odfdom.dom.element.style
            StyleParagraphPropertiesElement]
           [org.odftoolkit.odfdom.dom.element.text TextHElement TextLineBreakElement TextPElement]))

(def heading-formats {:h1 {:style-name "Heading_20_1" :outline-level 1}
                      :h2 {:style-name "Heading_20_2" :outline-level 2}
                      :h3 {:style-name "Heading_20_3" :outline-level 3}})

(def paragraph-style "Text_20_body")

(defn modify-paragraph-style [doc style-name]
  "we modify an existing paragraph style
  so that we will not get page breaks
  in the middle of a paragraph"
  (let [styles (.getStylesDom doc)
        all-styles (.getElementsByTagName styles "style:style")
        style (first
                (filter #(= (.getAttribute % "style:name") style-name)
                        (map #(.item all-styles %) (range (.getLength all-styles)))))]

    (if (nil? style)
      (do
        (println (str "Style not found: " style-name))
        doc)

      (let [props-node-list (.getElementsByTagName style "style:paragraph-properties")
            props (if (pos? (.getLength props-node-list))
                    (.item props-node-list 0)
                    (.newOdfElement styles StyleParagraphPropertiesElement))]

        ;; Set fo:keep-together
        (.setAttributeNS props
                         "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
                         "fo:keep-together"
                         "always")

        ;; Set fo:break-inside
        (.setAttributeNS props
                         "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
                         "fo:break-inside"
                         "avoid")

        ;; Attach props if not already present
        (when (zero? (.getLength props-node-list))
          (.appendChild style props))

        doc))))


(defn write-heading [doc format text]
  (let [dom (.getContentDom doc)
        heading (TextHElement. dom)]
    (.setTextContent heading text)
    (.setTextStyleNameAttribute heading (:style-name format))
    (.setTextOutlineLevelAttribute heading (int (:outline-level format)))
    (.appendChild (.getContentRoot doc) heading)
    doc))

(defn write-para [doc style-name text]
  (let [dom (.getContentDom doc)
        para (TextPElement. dom)
        lines (clojure.string/split text #"\n")]
    (.setStyleName para style-name)
    (doseq [[i line] (map-indexed vector lines)]
      (.appendChild para (.createTextNode dom line))
      (when (< i (dec (count lines)))
        (.appendChild para (.newOdfElement dom TextLineBreakElement))))
    (.appendChild (.getContentRoot doc) para)
    doc))

(defn write-ted-talk [input]
  (try
    (let [ted-talk (cond
                     (string? input) (edn/read-string (slurp (io/file input)))
                     (map? input) input
                     :else (throw (ex-info "Unsupported input type" {:type (type input)})))
          title (:title ted-talk)
          doc (-> (OdfTextDocument/newTextDocument)
                  (modify-paragraph-style paragraph-style)
                  (write-heading (:h1 heading-formats) (str "TED Talk: " title))
                  (write-para paragraph-style "")
                  (write-heading (:h3 heading-formats) (str "Speaker: " (:speaker ted-talk)))
                  (write-para paragraph-style "")
                  (write-heading (:h3 heading-formats) "Introduction")
                  (write-para paragraph-style (:description ted-talk))
                  (write-para paragraph-style "")
                  (write-heading (:h3 heading-formats) "Transcript"))
          final-doc (reduce (fn [d para]
                              (-> d
                                  (write-para paragraph-style (str (:timestamp para) "\n" (:content para)))
                                  (write-para paragraph-style "")))
                            doc
                            (:transcript ted-talk))]
      {:ok?    true
       :value {:title title :doc final-doc}})
    (catch Exception e
      {:ok?   false
       :error (.getMessage e)})))

(defn slugify [s]
  (-> s
      str/lower-case
      (str/replace #" " "_")))

(defn save-ted-talk [input folder]
  (try
    (let [write-result (write-ted-talk input)]
      (if-not (:ok? write-result)
        write-result
        (let [slug-title (slugify (get-in write-result [:value :title]))
              doc (get-in write-result [:value :doc])
              file (io/file folder (str slug-title ".odt"))
              path (.getPath file)]
          (.save doc path)
          {:ok? true :value path})))
    (catch Exception e
      {:ok?   false
       :error (.getMessage e)})))

(comment
  (save-ted-talk "data/sample-ted-talk.edn" "data")
  (nop))


