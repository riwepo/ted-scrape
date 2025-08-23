(ns ted-scrape.cli
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [ted-scrape.core :refer [process-ted-talk]])
  (:gen-class)
  (:import (java.io File)))

(defn valid-ted-transcript-url? [url]
  (let [pattern #"^https://www\.ted\.com/talks/([a-z0-9_\-]+)/transcript$"
        match (re-matches pattern url)]
    (boolean match)))

(defn valid-output-dir?
  "Returns true if the path exists, is a directory, and is writable."
  [^String path]
  (let [f (File. path)]
    (and (.exists f)
         (.isDirectory f)
         (.canWrite f))))

(def cli-options
  [["-u" "--url URL" "TED transcript URL"
    :validate [valid-ted-transcript-url?
               "Must be a valid TED transcript URL"]
    :missing "URL is required"]
   ["-o" "--output-dir DIR" "Output directory"
    :default "."
    :validate [valid-output-dir?
               "Must be a valid writable directory"]]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["This program parses a TED talk video transcript from a given URL."
        "The transcript is saved to a LibreOffice .odt file."
        ""
        "Usage: ted-scrape [options]"
        ""
        "Options:"
        options-summary
        ""]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (println "validate-args" args)
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)                                       ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors                                                ; errors => exit with description of errors
      {:exit-message (error-msg errors) :ok? false}
      :else                                                 ; run the program with options
      {:action "process-ted-talk" :options options})))

(defn do-exit [{:keys [exit err]}]
  (when err
    (println err))
    ;  (println exit))                                           ; in dev, keep repl running just print status
  (System/exit exit))

(defn -main [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (do-exit {:exit (if ok? 0 1) :err exit-message})
      (let [url (:url options)
            output-dir (:output-dir options)
            result (process-ted-talk output-dir url)]
        (let [{:keys [ok? error]} result]
          (do-exit {:exit (if ok? 0 1) :err error}))))))



(comment
  (-main
    "-o" "C:/temp"
    "-u" "https://www.ted.com/talks/eric_schmidt_the_ai_revolution_is_underhyped/transcript")

  (-main "-u" "https://www.ted.com/talks/eric_schmidt_the_ai_revolution_is_underhyped/transcript" "-o" "C:/temp")
  nil)



