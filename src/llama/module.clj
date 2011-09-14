(ns llama.module)

(def modules (atom [
                    'llama.modules.core-menus
;                    'llama.modules.document
                    'llama.modules.editor
                    ]))

(defn get-modules []
  @modules)

(defn init-modules []
  (doseq [m (get-modules)]
    (require m)
    (println m)
;    (if-not (:lib (meta (find-ns m)))
      ((ns-resolve m 'init-module))))

