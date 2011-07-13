(ns llama.error
  (:require [seesaw.core :as ssw]))

(defn- create-error-fn [message solution options]
  (fn [options]
    (let [{:keys [parent]} (apply hash-map options)]
      (ssw/alert parent (str message "\n\n" solution)))))

(let [errors (atom {})]
  (defn deferror [id message solution & options]
    {:pre [(not (contains? @errors id))]}
    (swap! errors assoc id (create-error-fn message solution options)))

  (defn show-error [id & options]
    ((get @errors id) options)))

;; errors

(deferror
  "no main in project"
  "Project does not define a main class"
  "Set \":main namespace\" in project.clj
to be able to run this project. Note that
the namespace must be gen-classed and 
contain a main function.")

