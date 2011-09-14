(ns llama.core
  (:use (llama [config :only [init-options listen-to-ui-update show-options-dialog]]
               [module :only [init-modules]]
               [util :only [set-focus]]
               [module-utils :only [get-menus get-views]]))
  (:require dynamik
            [llama.state :as state]
            [seesaw.core :as ssw])
  (:import java.awt.event.KeyEvent
           (javax.swing.text DefaultEditorKit$CutAction
                             DefaultEditorKit$CopyAction
                             DefaultEditorKit$PasteAction)))

(defn main-area []
  (let [views (get-views)]
    (dynamik/dynamik-panel
      :create-content #((get views %))
      :default-type "editor"
      :menu? false
      :types (keys views))))

(defn menubar [panel]
  (ssw/menubar :items (get-menus)))

(defn llama-editor []
  (init-options)
  (init-modules)
  (let [c (main-area)
        f (ssw/frame :content c :title "llama-editor" :size [800 :by 500] :menubar (menubar c))]
    (set-focus :panel (fn [_ e?] (.setEditable c e?)))
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
    (listen-to-ui-update f)
    (ssw/show! f)
    nil))

