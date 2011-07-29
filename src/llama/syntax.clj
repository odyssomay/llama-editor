(ns llama.syntax
    (:use [clojure.string :only (split)]
          (llama [lib :only [log]])))

(log :trace "started loading")

;; p-count function

(defn get-count-coll [in_str start end]
  (reductions (fn [sum next]
		(condp = next
		    start (inc sum)
		    end   (dec sum)
		    sum))
	      0 in_str))

(defn get-count [in_str start end]
  (last (get-count-coll in_str start end)))

(defn total-count [str_seq start end]
  (reduce + (map #(get-count % start end) str_seq)))

(defn find-count [in_str n start end]
  (let [raw_counts (get-count-coll in_str start end)
	offset (count (rest (take-while zero? raw_counts)))
	counts (drop-while zero? raw_counts)
	ans (+ (count (take-while
		       #(not= % n) counts))
	       offset)]
    (if (== ans (count in_str))
      nil
      ans)))

(defn find-matched [in_str start end]
  (find-count in_str 0 start end))

(defn find-unmatched-r [in_str start end]
  (find-count in_str -1 start end))

(defn find-unmatched-l [in_str start end]
  (let [raw_count
	(find-count (reverse in_str) 1 start end)]
    (if raw_count
      (dec (- (count in_str) raw_count)))))

;; parens

(defn parens-count [in_str]
  (get-count in_str \( \)))

(defn find-unmatched-rparens [in_str]
  (find-unmatched-r in_str \( \)))

(defn find-unmatched-lparens [in_str]
  (find-unmatched-l in_str \( \)))

(defn parens-indent [in_str]
  (if-let [indent (find-unmatched-lparens in_str)]
    (if (= (nth in_str (dec indent) nil) \')
      (inc indent)
      (+ 2 indent
	 (count (take-while #(not= % \space) (drop (inc indent) in_str)))))
    0))

;; bracket

(defn bracket-count [in_str]
  (get-count in_str \[ \]))

(defn find-unmatched-rbracket [in_str]
  (find-unmatched-r in_str \[ \]))

(defn find-unmatched-lbracket [in_str]
  (find-unmatched-l in_str \[ \]))

(defn bracket-indent [in_str]
  (if-let [indent (find-unmatched-lbracket in_str)]
    (inc indent)
    0))

;; cbracket

(defn cbracket-count [in_str]
  (get-count in_str \{ \}))

(defn find-unmatched-rcbracket [in_str]
  (find-unmatched-r in_str \{ \}))

(defn find-unmatched-lcbracket [in_str]
  (find-unmatched-l in_str \{ \}))

(defn cbracket-indent [in_str]
  (if-let [indent (find-unmatched-lcbracket in_str)]
    (inc indent)
    0))

;;

(defn get-coll-indent [count_f indent_f str_seq]
  (let [counts (reductions + (reverse (map count_f str_seq)))]
    (if (zero? (last counts))
      0
      (let [indent_line  (count (take-while #(<= % 0) counts))]
        (if (== indent_line (count str_seq))
          0
          (indent_f (nth (reverse str_seq) indent_line)))))))

;; get-indent function

(defn get-indent [str_seq]
  (max (get-coll-indent parens-count parens-indent str_seq)
       (get-coll-indent bracket-count bracket-indent str_seq)
       (get-coll-indent cbracket-count cbracket-indent str_seq)))

;; end get-indent

(defn indent [in_str]
  (let [indent (get-indent
		(split (str (last (split in_str #"\n[ ]*\n")))
		       #"\n"))]
    (apply str (cons \newline
		     (take indent (repeat \space))))))

(log :trace "finished loading")
