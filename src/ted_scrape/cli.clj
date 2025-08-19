(ns ted-scrape.cli
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [ted-scrape.utils :refer [valid-ted-transcript-url? valid-output-dir?]]
            [ted-scrape.core :refer [run]]))

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
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)                                       ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors                                                ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      :else                                                 ; run the program with options
      {:action "run" :options options})))

(defn exit [status msg]
  (println msg)
  (println status)) ; remove later
;(System/exit status))

(defn -main [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (run options))))

(comment
  ;(-main "--url" "https://www.ted.com/talks/slug/transcript")
  (-main "-u" "https://www.ted.com/talks/slug/transcript" "-o" "C:/temp")
  (nop))



