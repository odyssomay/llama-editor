(ns llama.main
  (:gen-class)
  (:use llama.lib)
  (:import (java.io IOException InputStreamReader OutputStreamWriter)))

(defn copy [input output]
  (-> (fn []
        (try 
          (.write output (.read input))
          (recur)
          (catch IOException _ )))
      (Thread.)
      .start))

(defn -main [& args]
  (let [process (start-process (str "java -cp " (.getProperty (System/getProperties) "java.class.path" nil)
                                    " clojure.main -"))]
    (copy (:input_stream process) *out*)
    (copy (:error_stream process) *err*)
    (doto (:output_stream process)
      (.write "(use 'llama.core) (llama-editor)")
      .flush)
    (.waitFor (:process process))
    (System/exit 0)))
