(ns llama.error
  (:use [clojure.string :only [split-lines]])
  (:require (seesaw [core :as ssw]
                    [mig :as ssw-mig])
            (llama [document :as document]))
  (:import javax.swing.UIManager))

(def ^{:private true} expanded-icon (UIManager/get "Tree.expandedIcon"))
(def ^{:private true} collapsed-icon (UIManager/get "Tree.collapsedIcon"))

(defn- create-error-fn [message solution options]
  (fn [options]
    (let [{:keys [parent]} (apply hash-map options)
          dialog (ssw/custom-dialog :parent parent)
          solution-panel (let [l (ssw/label :icon collapsed-icon :text "solution")
                               panel (ssw/vertical-panel :items [l])]
                           (ssw/listen l :mouse-clicked 
                                       (fn [_] 
                                         (ssw/config! l :icon expanded-icon)
                                         (ssw/config! panel :items [l (ssw/text :text solution :editable? false :border 10 :font (document/get-font)
                                                                                :multi-line? true :rows (count (split-lines solution)))])
                                         (ssw/pack! dialog)))
                           panel)]
      (ssw/config! 
        dialog
        :content 
        (ssw-mig/mig-panel :constraints ["gap 10 10" "align center"] :border 5 
          :items [[(ssw/label :text message :icon (ClassLoader/getSystemResource "icons/gnome_dialog_warning.png")) "wrap"]
                  [solution-panel "wrap"]
                  [(ssw/action :name "OK" :handler (fn [_] (ssw/dispose! dialog))) "wrap"]]))
      (ssw/pack! dialog)
      (ssw/show! dialog))))
;      (ssw/alert parent (str message "\n\n" solution)))))

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
  "Set \":main namespace\" in project.clj to be 
able to run this project. Note that
the namespace must be gen-classed 
and contain a main function.
  
Example for project ex-proj:

project.clj
  (defproject ex-proj
    :main ex-proj.core
    ...)
  
src/ex_proj/core.clj
  (ns ex-proj.core
    (:gen-class)
    ...)
  
  (defn -main [& args]
    ...)")

