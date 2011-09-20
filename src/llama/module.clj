(ns llama.module
  (:use (llama [util :only [log]]
               [module-utils :only [reset-views reset-menus]])))

(def modules 
  [{:id "menus"    :depends ["editor" "project" "repl"]}
   {:id "document" :depends ["ui"] :passive true}
   {:id "editor"   :depends ["document" "syntax" "code"]}
   {:id "project"  :depends ["editor" "repl"]}
   {:id "repl"     :depends ["syntax" "document"]}
   {:id "syntax"   :passive true}
   {:id "ui"}])

(def active-modules 
  (atom ["ui" "core-menus" "editor" 
         "project" "repl"
         ]))

(defn get-modules []
  @active-modules)

(defn init-modules []
  (log :info "initializing modules")
  (reset-views)
  (reset-menus)
  (doseq [m (get-modules)]
    (let [module-sym (symbol (str "llama.modules." m))]
      (require module-sym :reload)
      (println module-sym)
      (if-let [f (ns-resolve module-sym 'init-module)]
        (f)
        (log :error (str " is missing (init-module) in module: " m))))))

