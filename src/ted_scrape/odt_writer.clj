(ns ted-scrape.odt-writer
  (:import [org.odftoolkit.odfdom.doc OdfTextDocument]
           [org.odftoolkit.odfdom.dom.element.text TextHElement TextPElement]))

(defn write-heading2 [doc heading-text]
  (let [dom (.getContentDom doc)
        heading (TextHElement. dom)]
    (.setTextContent heading heading-text)
    (.setAttribute heading "text:style-name" "Heading_20_2")
    (.appendChild (.getContentRoot doc) heading)
    doc))                                                   ; return doc for chaining

(defn write-para [doc para-text]
  (let [dom (.getContentDom doc)
        para (TextPElement. dom)]
    (.setTextContent para para-text)
    (.setAttribute para "text:style-name" "Text_20_body")
    (.appendChild (.getContentRoot doc) para)
    doc))                                                   ; return doc for chaining

(defn richo2 []
  (-> (OdfTextDocument/newTextDocument)
      (write-heading2 "Chapter 1")
      (write-para "")
      (write-para "")
      (write-para "This is normal body text.")
      (write-para "")
      (write-para "With somme line breaks")
      (.save "data/test.odt")))

(comment
  (richo2)
  (create-doc)
  (nop))


