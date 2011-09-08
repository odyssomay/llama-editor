(ns llama.core
  (:use (llama [config :only [init-options listen-to-ui-update show-options-dialog]]
               [ui :only [init-ui]]
               [editor :only [editor-view]]
               [repl :only [repl-view]]
               [util :only [send-to-focus]]))
  (:require dynamik
            [llama.state :as state]
            [seesaw.core :as ssw])
  (:import java.awt.event.KeyEvent
           (javax.swing.text DefaultEditorKit$CutAction
                             DefaultEditorKit$CopyAction
                             DefaultEditorKit$PasteAction)))

(defn main-area []
  (dynamik/dynamik-panel
    :create-content (fn [type]
                      (case type
                        "editor" (editor-view)
                        "repl" (repl-view)))
    :default-type "editor"
    :menu? false
    :types ["editor" "repl" "project pane"]))

(defn file-menu []
  [(ssw/action :name "New" :tip "Create a new file" :mnemonic \n :key "menu N"
               :handler (fn [_] (send-to-focus :editor :new)))
   (ssw/action :name "Open" :tip "Open an existing file" :mnemonic \O :key "menu O"
               :handler (fn [_] (send-to-focus :editor :open)))
   :separator
   (ssw/action :name "Save" :mnemonic \S :key "menu S"
               :handler (fn [_] (send-to-focus :editor :save)))
   (ssw/action :name "Save As" :mnemonic \A :key "menu shift S"
               :handler (fn [_] (send-to-focus :editor :save-as)))
   :separator
   (ssw/action :name "Close" :tip "Close the current tab" :mnemonic \C :key "menu W"
               :handler (fn [_] (send-to-focus :editor :remove-current-tab)))])

(defn edit-menu [panel]
  [(ssw/menu-item :action (DefaultEditorKit$CutAction.) :text "Cut" :key "menu X")
   (ssw/menu-item :action (DefaultEditorKit$CopyAction.) :text "Copy" :key "menu C")
   (ssw/menu-item :action (DefaultEditorKit$PasteAction.) :text "Paste" :key "menu V")
   :separator
   (ssw/action :name "Undo" :key "menu Z" :handler (fn [_] (send-to-focus :editor :undo)))
   (ssw/action :name "Redo" :key "menu R" :handler (fn [_] (send-to-focus :editor :undo)))
   :separator
   (ssw/action :name "Indent" :key "menu I"
               :handler (fn [_] (send-to-focus :editor :indent)))
   (let [a (ssw/action :name "Indent right" 
                       :handler (fn [_] (send-to-focus :editor :indent-right)))]
     (.putValue a javax.swing.Action/ACCELERATOR_KEY
                (javax.swing.KeyStroke/getKeyStroke KeyEvent/VK_RIGHT KeyEvent/ALT_DOWN_MASK))
     a)
   (let [a (ssw/action :name "Indent left"
                       :handler (fn [_] (send-to-focus :editor :indent-left)))]
     (.putValue a javax.swing.Action/ACCELERATOR_KEY
                (javax.swing.KeyStroke/getKeyStroke KeyEvent/VK_LEFT KeyEvent/ALT_DOWN_MASK))
     a)
   :separator
   (ssw/checkbox-menu-item :action 
     (ssw/action :name "layout" :key "menu E"
       :handler (fn [e] (.setEditable panel (.isSelected (.getSource e))))))
   (ssw/action :name "Preferences"
               :handler (fn [_] (show-options-dialog)))])

(defn menubar [panel]
  (let [fm (ssw/menu :text "File" :items (file-menu))
        em (ssw/menu :text "Edit" :items (edit-menu panel))]
    (ssw/menubar :items [fm em])))

(defn llama-editor []
  (init-options)
  (init-ui)
  (let [c (main-area)
        f (ssw/frame :content c :title "llama-editor" :size [800 :by 500] :menubar (menubar c))]
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

