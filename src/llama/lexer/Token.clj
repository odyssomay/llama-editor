(ns llama.lexer.Token
  (:gen-class
    :init init
    :state state
    :constructors {[String Integer Integer Integer Integer] []}
    :methods [[type [] String]
              [start [] Integer]
              [length [] Integer]]
    ))

(defn -init [token_type line column offset length]
  [[] {:type token_type :line line 
       :column column :offset offset :length length}])

(defn -type [this]
  (:type (.state this)))

(defn -start [this]
  (:offset (.state this)))

(defn -length [this]
  (:length (.state this)))

(defn -equals [this obj]
  {:pre [(= (class obj) (class this))]}
  (let [s1 (.state this)
        s2 (.state obj)]
    (= (dissoc s1 :line :offset)
       (dissoc s2 :line :offset))))

