(ns llama.syntax
    (:use [clojure.string :only (split split-lines)]
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

(defn get-line-for-offset [string offset]
  (count (filter (partial = \newline) (subs string 0 offset))))

(defn get-line-start-offset [string line]
  (let [lines (map #(str % "\n") (split-lines string))]
    (reduce + (map count (take line lines)))))

(defn get-line-offset [string offset]
  (- offset (get-line-start-offset string (get-line-for-offset string offset))))

;; parens

(defn parens-count [in_str]
  (get-count in_str \( \)))

(defn find-unmatched-rparens [in_str]
  (find-unmatched-r in_str \( \)))

(defn find-unmatched-lparens [in_str]
  (find-unmatched-l in_str \( \)))

(defn parens-indent [string & [dynamic-indent?]]
  (if-let [unmatched (find-unmatched-lparens string)]
    (let [indent (get-line-offset string unmatched)
          in_str (subs string (get-line-start-offset string (get-line-for-offset string unmatched)))]
      (if (= (nth in_str (dec indent) nil) \')
        (inc indent)
        (+ 2 indent
           (if dynamic-indent?
             (count (take-while #(not= % \space) (drop (inc indent) in_str)))
             0)
           )))
    0))

;; bracket

(defn bracket-count [in_str]
  (get-count in_str \[ \]))

(defn find-unmatched-rbracket [in_str]
  (find-unmatched-r in_str \[ \]))

(defn find-unmatched-lbracket [in_str]
  (find-unmatched-l in_str \[ \]))

(defn bracket-indent [in_str]
  (if-let [unmatched (find-unmatched-lbracket in_str)]
    (let [indent (get-line-offset in_str unmatched)]
      (inc indent))
    0))

;; cbracket

(defn cbracket-count [in_str]
  (get-count in_str \{ \}))

(defn find-unmatched-rcbracket [in_str]
  (find-unmatched-r in_str \{ \}))

(defn find-unmatched-lcbracket [in_str]
  (find-unmatched-l in_str \{ \}))

(defn cbracket-indent [in_str]
  (if-let [unmatched (find-unmatched-lcbracket in_str)]
    (let [indent (get-line-offset in_str unmatched)]
      (inc indent))
    0))

;;

(defn get-indent [string & [dynamic-indent?]]
  (max (parens-indent string dynamic-indent?)
       (bracket-indent string)
       (cbracket-indent string)))

(defn indent [in_str & [dynamic-indent?]]
  (let [indent (get-indent (str (last (split in_str #"\n[ ]*\n"))) dynamic-indent?)]
    (apply str (cons \newline
		     (take indent (repeat \space))))))

(log :trace "finished loading")
