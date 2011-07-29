(ns llama.state
  (:use [clojure.java.io :only [file]]
        [llama [lib :only [log]]])
  (:import (java.io BufferedInputStream BufferedOutputStream
                    FileInputStream FileOutputStream)))

(log :trace "started loading")

(def state-file (file "state"))
(def beans-file (file state-file "beans"))

(doseq [f [state-file beans-file]]
  (if-not (.exists f)
    (.mkdir f)))

;; states

(defn load-state 
  ([id]
   (load-state id identity))
  ([id if-found]
   (try 
     (let [f (file state-file (name id))]
       (if (.exists f)
         (if-found (read-string (slurp f)))))
     (catch Exception e
       (log :error e (str "unable to recover state " id))))))

(defn save-state [id state]
  (try 
    (let [f (file state-file (name id))]
      (with-open [out (java.io.FileWriter. f)]
        (binding [*out* out]
          (pr state))))
    (catch Exception e
      (log :error e (str "unable to save state " id)))))

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
   (try
     (let [f (file beans-file (name id))]
       (if (.exists f)
         (with-open [d (java.beans.XMLDecoder. (BufferedInputStream. (FileInputStream. f)))]
           (if-found (.readObject d)))))
     (catch Exception e
       (log :error e (str "unable to recover bean " id))))))

(defn save-bean [id b]
  (try 
    (let [f (file beans-file (name id))]
      (with-open [enc (java.beans.XMLEncoder. (BufferedOutputStream. (FileOutputStream. f)))]
        (.writeObject enc b)))
    (catch Exception e
      (log :error e (str "unable to save bean " id)))))

(let [beans (atom [])]
  (defn defbean [id b]
    (swap! beans conj [(name id) b]))

  (defn save-bean-states []
    (doseq [[id b] @beans]
      (save-bean id (b)))))

(log :trace "finished loading")
