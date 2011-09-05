(ns llama.config
  (:use [llama.lib :only [log]])
  (:require [seesaw.core :as ssw]))

(defn load-config-files [& files]
  (dorun (map #(load (str "/config/" %)) files)))

(defn edit-config-order [& [parent]]
  )

(defn load-config []
  (apply load-config-files
         (map str
              (read-string 
                (str "[" (slurp (.getPath (ClassLoader/getSystemResource "config-order")))
                     "]")))))

;; options

(def options (atom {}))

(defn get-option* [class-id id]
  (get-in @options [class-id id]))

(defn set-option [class-id id value]
  (swap! options assoc-in [class-id id] value))

(defn listen-to-option
  ([class-id f]
   (add-watch options (gensym) 
     (fn [_ _ old-item item]
       (let [o (get old-item class-id)
             n (get item class-id)]
         (if-not (= o n)
           (f o n))))))
  ([class-id id f]
   (listen-to-option class-id
     (fn [old-item item]
       (let [o (get old-item id)
             n (get item id)]
       (if-not (= o n)
         (f o n)))))))

(def options-dialog
  (ssw/frame))

(defn show-options-dialog []
  (-> options-dialog ssw/pack! ssw/show!))
