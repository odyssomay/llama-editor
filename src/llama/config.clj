(ns llama.config)

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
