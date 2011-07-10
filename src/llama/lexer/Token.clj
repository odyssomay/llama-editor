(ns llama.lexer.Token
  (:gen-class
    :init init
    :state state
    :constructors {[String Integer Integer Integer] []}
    :methods [ 
              ;#^{:static true} [createToken [String Integer Integer] clojure.lang.PersistentHashMap]]
;              [type [] String]
;              [start [] Integer]
;              [length [] Integer]
    ]
    ))

(defn -init [token_type line column length]
  [[] {:type token_type :line line :column column :length length}])

(defn -type [this]
  (:type (.state this)))

(defn -start [this]
  )

(defn -length [this] 
  )

(defn -equals [this obj]
  {:pre [(= (class obj) (class this))]}
  (let [s1 (.state this)
        s2 (.state obj)]
    (= (dissoc s1 :line)
       (dissoc s2 :line))))

