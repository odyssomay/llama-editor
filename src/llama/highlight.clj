(ns llama.highlight
  (:use (llama lib))
  (:import (llama.lexer ClojureLexer)
;	   (jsyntaxpane Token TokenType)
	   (java.io StringReader)))

;; the only code used here is the SCANNER section below, the rest
;; is kept because of various reasons. 

;; FUNCTIONAL

(defn- get-bounds [str_seq n]
      "Calculates the first and end index of the nth element in a seq of strings."
      (let [counts (map count (take (inc n) str_seq))
            sum_counts (reduce + counts)]
           (list (- sum_counts (last counts)) sum_counts)))

(defn- split-and-keep [fn in_str]
  "Splits a string with fn and keeps all the parts."
  (map #(apply str %) (partition-by fn in_str)))

(defn slice-clojure-code [text]
  (split-and-keep #(nil? (re-matches #"[(){}\[\]\"\"\s]" (str %))) text))

(defn word-highlight [in_str keywords]
      "Evals to a list with the start and end index of the keywords in the in_str."
      (let [split_str   (slice-clojure-code in_str)
            highlighted (filter (complement nil?)
                                (map-indexed (fn [x y] (if (some #(= % y) keywords) (get-bounds split_str x)))
                                             split_str))]
           highlighted))

(defn str-highlight [in_str]
  "highlights strings in in_str"
  (let [split_str (let [op (split-and-keep #(= % \") in_str)] (if (= (first op) "\"") (cons "" op) op))
	highlighted (filter (complement nil?)
			    (map-indexed (fn [x y] (if (and (pos? (rem x 4)) (zero? (rem x 2))) 
						     (get-bounds split_str
								 x)))
					 split_str))]
    highlighted))

;; IMPERATIVE

(defn text [doc]
  (.getText doc 0 (.getLength doc)))
  
(defn set-attribute [doc style pos]
      "Set attribute with position specified in pos to doc."
      (.setCharacterAttributes doc (first pos) (second pos) style true))

(defn set-attributes [doc style in_pos]
      "Set attributes with positions specified in pos to doc."
      (doseq [pos in_pos] (set-attribute doc style (list (first pos) (- (second pos) (first pos))))))

(defn reset-attributes [doc style]
      "Resets all attributes (highlighting) in doc."
      (.setCharacterAttributes doc 0 (.getLength doc) style true))

(defn highlight-words [document style keywords]
      "Highlights the keywords in document with style. (yeah)"
      (let [text (.getText document 0 (.getLength document))]
           (set-attributes document style (word-highlight text keywords))))

(defn highlight-str [doc style]
  (let [text (text doc)]
    (set-attributes doc style (str-highlight text))))

;; NEW

(def get-keywords
  (memoize (fn []
	     (concat (map str (keys (ns-publics 'clojure.core)))
		     '("def" "if" "do" "let"
		       "quote" "var" "fn" "loop"
		       "recur" "throw" "try"
		       "monitor-enter" "monitor-exit")))))

(defn get-highlight-rules []
  [[(fn [x] (some #(= x %) (get-keywords))) "core"]
   [(fn [x] (re-matches #"\\\w" x)) "char"]
   [(fn [x] true) "main"]])
   
(defn what-highlight [text str-count]
  (if (odd? str-count)
    "str"
    (let [rules (get-highlight-rules)
	  results (map (fn [x] (if ((first x) text) (second x))) rules)]
       (first (remove nil?
		      results)))))

(defn get-highlight [offset text str_count]
  (let [pos [offset (+ offset (count text))]
	style (what-highlight text str_count)]
    [style pos]))

(defn get-highlight-last [offset text str_count]
  (let [split_text (slice-clojure-code text)
	but_last_text (apply str (drop-last split_text))
	new_offset (+ offset (count but_last_text))
	new_str_count (+ str_count
			 (count (filter #(= \" %) but_last_text)))]
    (get-highlight new_offset (last split_text) new_str_count)))

(defn merge-highlight [highlight]
  (map (fn [x]
	 [(ffirst x) (map last x)])
       (partition-by first (sort-by first highlight))))

(defn get-text-highlight [offset text str_count]
  (let [split_text (slice-clojure-code text)
	last_text (map #(apply str %) (subseqs split_text))]
    (merge-highlight
     (map #(get-highlight-last offset % str_count) last_text))))

;; SCANNER

(defn get-tokens [lexer in_str]
  (let [reader (StringReader. in_str)]
    (.yyreset lexer reader)
    (loop [ans []]
      (let [next (.yylex lexer)]
	(if next
	  (recur (conj ans next))
	  ans)))))

(defn get-token-highlight [text lexer]
  (let [tokens (get-tokens lexer text)]
    (apply concat
           (map 
             #(map (fn [x] [(.toString (.type x)) (.start x) (.length x)]) %) 
             tokens))))

(let [lexer (ClojureLexer. (StringReader. ""))]
  (defn clj-highlight [text]
    (get-token-highlight text lexer)))

