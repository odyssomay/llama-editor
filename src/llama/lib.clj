(ns llama.lib
  (:import (java.io OutputStreamWriter InputStreamReader)
	   (javax.swing JTabbedPane)))

(defn drop-n [coll n]
  "Drop (remove) the n item in coll."
  (concat (take n coll) (drop (inc n) coll)))

(defn split-seq [coll item]
  "Split seq with item."
  (remove #(= % `(~item)) (partition-by #(= % item) coll)))

(defn insert [coll n item]
  "Insert item at the nth index."
  (concat (take n coll) [item] (drop n coll)))

(defn remove-n [coll n length]
  "Remove length items from coll, starting at n"
  (concat (take n coll) (drop (+ n length) coll)))

(defn find-i
  "Find the index of value in coll"
  [value coll]
  (let [limit (count coll)]
    (loop [i 0
           c coll]
      (if (== i limit)
        nil
        (if (= (first c) value)
          i
          (recur (inc i) (rest c)))))))

(defn replace-i
  "change index n in coll to value"
  [n value coll]
  (concat (take n coll)
          [value]
          (drop (inc n) coll)))

(defn change-i
  "change index n in coll with f"
  [n f coll]
  (replace-i n (f (nth coll n)) coll))

(defn subseqs [coll]
  (map (fn [x] (take (inc x) coll)) (range (count coll))))

(defn start-process [command]
  (let [runtime (Runtime/getRuntime)
	process (.exec runtime command)]
    (.addShutdownHook runtime (Thread. #(.destroy process)))
    {:process process
     :input_stream (InputStreamReader. (.getInputStream process))
     :output_stream (OutputStreamWriter. (.getOutputStream process))
     :error_stream (InputStreamReader. (.getErrorStream process))}))

(defn write-stream [stream str]
  (.write stream str)
  (.flush stream))

(defn read-stream [stream]
  (apply str
	 (map char
	      (loop [res []]
		(if (.ready stream)
		  (recur (concat res [(.read stream)]))
		  res)))))

(defn init-tabbed-pane [& tabs]
  (let [tabbed_pane (JTabbedPane.)]
    (dorun (map #(.addTab tabbed_pane (first %) (second %)) tabs))
    tabbed_pane))

(defn ignore [f]
  (fn [& _] (f)))
