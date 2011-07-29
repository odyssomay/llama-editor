(ns llama.main
  (:use [clojure.tools.logging :only [trace]])
  (:gen-class))

(defn -main [& args]
  (trace (str "starting llama editor, "
              (.getTime (java.util.Calendar/getInstance))))
  (eval '(do
           (use 'llama.core)
           (llama-editor))))

