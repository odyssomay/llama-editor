(defproject 
  llama "1.0.0-SNAPSHOT"
  :description "Llama is a lightweight editor for the clojure language."
  :main llama.core
  :resources-path "icons"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/tools.logging "0.1.2"]
                 [hafni "1.0.6-SNAPSHOT"]
                 [hafni-seesaw "1.0.2-SNAPSHOT"]
                 [clj-arrow "1.0.2-SNAPSHOT"]
                 [seesaw "1.0.7"]
                 [leiningen "1.6.1"]])
