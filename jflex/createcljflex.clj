(use '[clojure.string :only [join]])

(def text (slurp "clojure_raw.flex"))

(def keywords 
  (concat '[def if do let quote var fn loop recur throw try monitor-enter monitor-exit]
          (keys (ns-publics 'clojure.core))))

(spit "clojure.flex"
      (.replaceAll text
        "==INSERT_KEYWORDS_HERE=="
        (join " |\n" 
              (map #(str "\"(" % " \"") keywords))))

