(defn get-tokens [lexer in_str]
  (let [reader (java.io.StringReader. in_str)]
    (.yyreset lexer reader)
    (loop [ans []]
      (let [next (.yylex lexer)]
        (if next
          (recur (conj ans next))
          ans)))))

(println (map #(.state %) (get-tokens (llama.lexer.ClojureLexer.) 
"(defn a [] ; define fn a
       3)   ; as returning 3)")))
