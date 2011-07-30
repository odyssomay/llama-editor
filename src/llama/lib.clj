(ns llama.lib
  (:use [clojure.java.io :only [file]])
  (:require [clojure.tools.logging :as logger]
            [seesaw [core :as ssw]
                    [chooser :as ssw-chooser]])
  (:import (java.io OutputStreamWriter InputStreamReader)
           (javax.swing JTabbedPane)
           java.io.File
           (java.awt Color Font GraphicsEnvironment)))

(logger/log :trace "started loading")

(defn drop-nth [coll n]
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

;; taken from hafni

(def *available-fonts*
    (vec (.getAvailableFontFamilyNames (GraphicsEnvironment/getLocalGraphicsEnvironment))))

(def *path-separator* (File/separator))

(defn font [name size]
  (if-not (some (partial = name) *available-fonts*)
    (println "Font family doesn't exist - using default."))
  (Font. name Font/PLAIN size))

(defn color
  "Returns an instance of java.awt.Color.
  The three argument version takes red, green, blue.
  The one argument version takes a string, which is any of:
  black, blue, cyan, dark_gray, gray, green, light_gray,
  magenta, orange, pink, red, white, yellow."
  ([r g b]
   (Color. r g b))
  ([name]
   (case name
     "black" Color/black
     "blue" Color/blue
     "cyan" Color/cyan
     "dark_gray" Color/darkGray
     "gray" Color/gray
     "green" Color/green
     "light_gray" Color/lightGray
     "magenta" Color/magenta
     "orange" Color/orange
     "pink" Color/pink
     "red" Color/red
     "white" Color/white
     "yellow" Color/yellow)))

(defmacro log 
  ([level msg]
   `(logger/log ~level ~msg))
  ([level e msg]
   `(logger/log ~level (str ~msg "\n    " 
                            (.getMessage ~e) "\n        "
                            (apply str (interpose "\n        " 
                                                  (take 7 (.getStackTrace ~e))))
                            "\n"))))

(defn new-file-dialog [& [parent]]
  (let [filename (ssw/text)
        dir (ssw/text :text (System/getProperty "user.home"))
        panel (ssw/border-panel 
                :north filename :center dir 
                :east (ssw/action :name "browse" :handler (fn [_]
                                                            (.setText dir
                                                                      (.getCanonicalPath
                                                                        (ssw-chooser/choose-file
                                                                          :type "Ok" :selection-mode :dirs-only))))))
        dialog (ssw/dialog :content panel :option-type :ok-cancel
                            :modal? true)]
    (.setResizable dialog false)
    (when (-> dialog ssw/pack! ssw/show!)
      (file (.getText dir) (.getText filename)))))


(log :trace "finished loading")
