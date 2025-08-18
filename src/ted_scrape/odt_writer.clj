(ns ted-scrape.odt-writer
  (:require [clojure.edn :as edn])
  (:import [org.odftoolkit.odfdom.doc OdfTextDocument]
           [org.odftoolkit.odfdom.dom.element.text TextHElement TextPElement]))

(def heading-formats {:h1 {:style-name "Heading_20_1" :outline-level 1}
                      :h2 {:style-name "Heading_20_2" :outline-level 2}
                      :h3 {:style-name "Heading_20_3" :outline-level 3}})

(def style-names {:body "Text_20_body"})

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
        para (TextPElement. dom)]
    (.setTextContent para text)
    (.setTextStyleNameAttribute para style-name)
    (.appendChild (.getContentRoot doc) para)
    doc))

(defn write-ted-talk [ted-talk]
  (let [doc (-> (OdfTextDocument/newTextDocument)
                (write-heading (:h1 heading-formats) (str "TED Talk: " (:title ted-talk)))
                (write-para (:body style-names) "")
                (write-heading (:h3 heading-formats) (str "Speaker: " (:speaker ted-talk)))
                (write-para (:body style-names) "")
                (write-heading (:h3 heading-formats) "Introduction")
                (write-para (:body style-names) (:description ted-talk))
                (write-para (:body style-names) "")
                (write-heading (:h3 heading-formats) "Transcript"))]
    (reduce (fn [d para]
              (write-para d (:body style-names) (:timestamp para))
              (write-para d (:body style-names) (:content para))
              (write-para d (:body style-names) ""))
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


