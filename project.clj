(defproject 
  llama "1.0.0-SNAPSHOT"
  :description "Llama is a lightweight editor for the clojure language."
  :main llama.main
  :resources-path "resources"
  :aot [llama.lexer.Token]
  :manifest ["SplashScreen-Image" "llama-splash.gif"]
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [clj-arrow "1.0.3-SNAPSHOT"]
                 [seesaw "1.0.10"]
                 [leiningen "1.6.1"]
                 [slamhound "1.1.1"]]
  :dev-dependencies [[com.stuartsierra/lazytest "1.1.2"]])
