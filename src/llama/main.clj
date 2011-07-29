(ns llama.main
  (:gen-class))

(defn -main [& args]
  (eval '(do (use 'llama.core)
             (llama-editor))))
