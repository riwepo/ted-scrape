(ns ted-scrape.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [ted-scrape.core :refer :all]))


(def sample-p-tag
  {:type :element, :attrs nil, :tag :p, :content ["Hello world"]})

(def sample-doc
  {:type    :document,
   :content [{:type    :element,
              :attrs   nil,
              :tag     :html,
              :content [{:type :element, :attrs nil, :tag :head, :content nil}
                        {:type    :element,
                         :attrs   nil,
                         :tag     :body,
                         :content [{:type :element, :attrs nil, :tag :p, :content ["Hello world"]}]}]}]})


(deftest test-format-node-sample-p
  (let [formatted (format-node sample-p-tag 2)]
    (println formatted)
    (is (str/includes? formatted "<p>"))
    (is (str/includes? formatted "Hello world"))
    (is (str/includes? formatted "</p>"))))

(deftest test-format-node-sample-doc
  (let [formatted (format-node sample-doc 2)]
    (println formatted)
    (is (str/includes? formatted "<html>"))
    (is (str/includes? formatted "<head></head>"))
    (is (str/includes? formatted "<body>"))
    (is (str/includes? formatted "<p>"))
    (is (str/includes? formatted "Hello world"))
    (is (str/includes? formatted "</p>"))
    (is (str/includes? formatted "</body>"))
    (is (str/includes? formatted "</html>"))))

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

(deftest test-format-node
  (let [tree (extract-tree sample-html)
        html-node (first (:content tree))
        body-node (some #(when (= (:tag %) :body) %) (:content html-node))
        div-node (some #(when (and (= (:tag %) :div)
                                   (= (get-in % [:attrs :class]) "transcript"))
                          %)
                       (:content body-node))]
    (is div-node)
    (is (= (:tag div-node) :div))
    (let [formatted (format-node div-node 2)]
      (println formatted)
      (is (str/includes? formatted "<div>"))
      (is (str/includes? formatted "<p>"))
      (is (str/includes? formatted "Hello"))
      (is (str/includes? formatted "Another paragraph.")))))

