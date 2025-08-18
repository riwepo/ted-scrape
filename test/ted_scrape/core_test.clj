(ns ted-scrape.core-test
  (:require [clojure.test :refer :all]
            [ted-scrape.extract :refer :all]))

(def sample-html
  "<html>
     <head></head>
     <body>
       <div class='transcript'>
         <p>Hello <strong>world</strong>.</p>
         <p>Another paragraph.</p>
       </div>
     </body>
   </html>")

(deftest test-extract-tree
  (let [tree (extract-tree sample-html)]
    (println tree)
    (is (= (:type tree) :document))
    (let [content (:content tree)])
    (is (some #(= (:tag %) :html) (:content tree)))))



