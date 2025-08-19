(ns ted-scrape.odt-writer
  (:require [clojure.edn :as edn])
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

(defn write-ted-talk [ted-talk]
  (let [doc (-> (OdfTextDocument/newTextDocument)
                (modify-paragraph-style paragraph-style)
                (write-heading (:h1 heading-formats) (str "TED Talk: " (:title ted-talk)))
                (write-para paragraph-style "")
                (write-heading (:h3 heading-formats) (str "Speaker: " (:speaker ted-talk)))
                (write-para paragraph-style "")
                (write-heading (:h3 heading-formats) "Introduction")
                (write-para paragraph-style (:description ted-talk))
                (write-para paragraph-style "")
                (write-heading (:h3 heading-formats) "Transcript"))]
    (reduce (fn [d para]
              (write-para d paragraph-style (str (:timestamp para) "\n" (:content para)))
              (write-para d paragraph-style ""))
            doc
            (:transcript ted-talk))))

(defn richo []
  (let [raw-str (slurp "data/ted-talk.edn")
        ted-talk (edn/read-string raw-str)
        doc (write-ted-talk ted-talk)]
    (.save doc "data/test.odt")))

(comment
  (richo)
  (nop))


