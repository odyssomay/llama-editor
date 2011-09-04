(ns llama.core
  (:use (llama [editor :only [editor-view]]
               [repl :only [repl-view]]))
  (:require dynamik
            [seesaw.core :as ssw]))

(defn main-area []
  (dynamik/dynamik-panel
    :create-content (fn [type]
                      (case type
                        "editor" (editor-view)
                        "repl" (repl-view)))
    :default-type "editor"
    :types ["editor" "repl" "project pane"]))

(defn llama-editor []
  (ssw/show! (ssw/frame :content (main-area))))
