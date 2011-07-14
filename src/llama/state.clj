(ns llama.state
  (:use [clojure.java.io :only [file]])
  (:import (java.io BufferedInputStream BufferedOutputStream
                    FileInputStream FileOutputStream)))

;; states

(defn load-state 
  ([id]
   (load-state id identity))
  ([id if-found]
   (let [f (file (.getPath (ClassLoader/getSystemResource "state")) (name id))]
     (if (.exists f)
       (if-found (read-string (slurp f)))))))

(defn save-state [id state]
  (let [f (file (.getPath (ClassLoader/getSystemResource "state")) (name id))]
    (with-open [out (java.io.FileWriter. f)]
      (binding [*out* out]
        (pr state)))))

(let [states (atom [])]
  (defn defstate [id f]
    (swap! states conj [(name id) f]))
  
  (defn save-states []
    (doseq [[id f] @states]
      (save-state id (f)))))

;; beans

(defn load-bean
  ([id]
   (load-bean id identity))
  ([id if-found]
   (let [f (file (.getPath (ClassLoader/getSystemResource "beans")) (name id))]
     (if (.exists f)
       (with-open [d (java.beans.XMLDecoder. (BufferedInputStream. (FileInputStream. f)))]
         (if-found (.readObject d)))))))

(defn save-bean [id b]
  (let [f (file (.getPath (ClassLoader/getSystemResource "beans")) (name id))]
    (with-open [enc (java.beans.XMLEncoder. (BufferedOutputStream. (FileOutputStream. f)))]
      (.writeObject enc b))))

(let [beans (atom [])]
  (defn defbean [id b]
    (swap! beans conj [(name id) b]))

  (defn save-bean-states []
    (doseq [[id b] @beans]
      (save-bean id (b)))))

