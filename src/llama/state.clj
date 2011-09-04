(ns llama.state
  (:use [clojure.java.io :only [file]]
        [llama [lib :only [log]]])
  (:import (java.io BufferedInputStream BufferedOutputStream
                    FileInputStream FileOutputStream)))

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
         (let [content (read-string (slurp f))]
           (if-found content)
           content)))
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

(let [states (atom {})]
  (defn defstate [id f]
    (swap! states assoc (name id) f))
  
  (defn save-states []
    (doseq [[id f] @states]
      (save-state id (f))))

  (defn save-defined-state [id]
    (if (contains? @states (name id))
      (save-state id ((get @states (name id))))
      (log :error (str "state not defined: " id)))))

;; beans

(defn load-bean
  ([id]
   (load-bean id identity))
  ([id if-found]
   (try
     (let [f (file beans-file (name id))]
       (if (.exists f)
         (with-open [d (java.beans.XMLDecoder. (BufferedInputStream. (FileInputStream. f)))]
           (let [content (.readObject d)]
             (if-found content)
             content))))
     (catch Exception e
       (log :error e (str "unable to recover bean " id))))))

(defn save-bean [id b]
  (try 
    (let [f (file beans-file (name id))]
      (with-open [enc (java.beans.XMLEncoder. (BufferedOutputStream. (FileOutputStream. f)))]
        (.writeObject enc b)))
    (catch Exception e
      (log :error e (str "unable to save bean " id)))))

(let [beans (atom {})]
  (defn defbean [id b]
    (swap! beans assoc (name id) b))

  (defn save-bean-states []
    (doseq [[id b] @beans]
      (save-bean id (b))))
  
  (defn save-defined-bean-state [id]
    (if (contains? @beans (name id))
      (save-bean id ((get @beans (name id))))
      (log :error (str "bean not defined: " id)))))

