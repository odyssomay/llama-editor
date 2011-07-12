(require 'clojure.main)

;(defn init []
;  (let [out *out*]
;    (def *out* 
;      (proxy [java.io.Writer] []
;        (write ([input offset length] )
;               ([input]))
;        (flush [] (.flush out))
;        (close [] (.close out))))))
;

(defn caught [e]
      (println (.toString (clojure.main/repl-exception e)))
      (.printStackTrace e *err*)
      (.print *err* "\n")) 	

(def out (java.io.OutputStreamWriter. System/out))
(.write out "hello world")

(with-open [writer
            (proxy [java.io.Writer] []
              (write ([input offset length]
                      (.write this (subs (str input) offset (+ offset length))))
                     ([input]
                      (if (number? input)
;                        (if (> input 0)
                          (.print System/out (if (= (char input) \newline) "\n;" (char input)))
                        (.print System/out (.replaceAll (str input) "\n" "\n;")))))
              (flush [] 
                     ;(.flush *out*)
                     )
              (close [] 
                     ;(.close *out*)
                     ))
            ]
;  (binding [*out* writer]
    (clojure.main/repl :caught caught
                       :print #(do 
                                 (print ";=> ") 
                                 (prn %))
                       :prompt #()
                       ))
