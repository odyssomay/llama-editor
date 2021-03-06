(require 'clojure.main)

(println ";" *ns*)

(defn caught [e]
      (println (.toString (clojure.main/repl-exception e)))
      (.printStackTrace e *err*)
      (.print *err* "\n")) 	

(with-open [writer
            (proxy [java.io.Writer] []
              (write ([input offset length]
                      (.write this (subs (str input) offset (+ offset length))))
                     ([input]
                      (if (number? input)
                        (.print System/out (if (= (char input) \newline) "\n;" (char input)))
                        (.print System/out (.replaceAll (str input) "\n" "\n;")))))
              (flush [] )
              (close [] ))]
;  (binding [*out* writer]
    (clojure.main/repl :caught caught
                       :print #(do 
                                 (print ";=> ") 
                                 (prn %))
                       :prompt #()
                       ))
