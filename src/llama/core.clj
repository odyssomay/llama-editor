(ns llama.core
  (:use (llama [editor :only [editor-view]]
               [repl :only [repl-view]]))
  (:require dynamik
            [llama.state :as state]
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
  (let [c (main-area)
        f (ssw/frame :content c :title "llama-editor" :size [800 :by 500])]
    (state/defstate :main-layout #(.getTileLayout c))
    (state/load-state :main-layout #(.setTileLayout c %))
    (state/defstate :frame
      (fn [] [:size [(.getWidth f) :by (.getHeight f)]]))
    (state/load-state :frame
      (fn [state] (apply ssw/config! f state)))
    (state/defbean :frame-location #(.getLocation f))
    (state/load-bean :frame-location #(.setLocation f %))
    (ssw/listen
      f :window-closing
      (fn [_]
        (state/save-states)
        (state/save-bean-states)
        ;(System/exit 0)
        ))
    (ssw/show! f)))
