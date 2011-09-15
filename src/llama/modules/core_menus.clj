(ns llama.modules.core-menus
  (:use (llama [config :only [show-options-dialog]]
               [module-utils :only [add-menu send-to-module]]
               ))
  (:require 
    (llama.modules
      [editor :as editor])
    [seesaw.core :as ssw])
  (:import java.awt.event.KeyEvent
           (javax.swing.text DefaultEditorKit$CutAction
                             DefaultEditorKit$CopyAction
                             DefaultEditorKit$PasteAction)))

(defn file-menu []
  [(ssw/action :name "New" :tip "Create a new file" :mnemonic \n :key "menu N"
               :handler (fn [_] (send-to-module :editor :new)))
   (ssw/action :name "Open" :tip "Open an existing file" :mnemonic \O :key "menu O"
               :handler (fn [_] (editor/open-and-choose-file editor/current-tabs)))
   :separator
   (ssw/action :name "New project"
               :handler (fn [_] (send-to-module :project :new)))
   (ssw/action :name "Open project" :mnemonic \p
               :handler (fn [_] (send-to-module :project :open)))
   :separator
   (ssw/action :name "Save" :mnemonic \S :key "menu S"
               :handler (fn [_] (send-to-module :editor :save)))
   (ssw/action :name "Save As" :mnemonic \A :key "menu shift S"
               :handler (fn [_] (send-to-module :editor :save-as)))
   :separator
   (ssw/action :name "Close" :tip "Close the current tab" :mnemonic \C :key "menu W"
               :handler (fn [_] (send-to-module :editor :remove-current-tab)))])

(defn edit-menu []
  [(ssw/menu-item :action (DefaultEditorKit$CutAction.) :text "Cut" :key "menu X")
   (ssw/menu-item :action (DefaultEditorKit$CopyAction.) :text "Copy" :key "menu C")
   (ssw/menu-item :action (DefaultEditorKit$PasteAction.) :text "Paste" :key "menu V")
   :separator
   (ssw/action :name "Undo" :key "menu Z" :handler (fn [_] (send-to-module :editor :undo)))
   (ssw/action :name "Redo" :key "menu R" :handler (fn [_] (send-to-module :editor :undo)))
   :separator
   (ssw/action :name "Indent" :key "menu I"
               :handler (fn [_] (send-to-module :editor :indent)))
   (let [a (ssw/action :name "Indent right" 
                       :handler (fn [_] (send-to-module :editor :indent-right)))]
     (.putValue a javax.swing.Action/ACCELERATOR_KEY
                (javax.swing.KeyStroke/getKeyStroke KeyEvent/VK_RIGHT KeyEvent/ALT_DOWN_MASK))
     a)
   (let [a (ssw/action :name "Indent left"
                       :handler (fn [_] (send-to-module :editor :indent-left)))]
     (.putValue a javax.swing.Action/ACCELERATOR_KEY
                (javax.swing.KeyStroke/getKeyStroke KeyEvent/VK_LEFT KeyEvent/ALT_DOWN_MASK))
     a)
   :separator
   (ssw/action :name "Find/replace" :key "menu S"
     :handler (fn [_] (send-to-module :editor :find-replace)))
   :separator
   (ssw/checkbox-menu-item :action 
     (ssw/action :name "layout" :key "menu E"
       :handler (fn [e] (send-to-module :panel :editable? (.isSelected (.getSource e))))))
   (ssw/action :name "Preferences"
               :handler (fn [_] (show-options-dialog)))])

(defn init-module []
  (add-menu "File" (file-menu))
  (add-menu "Edit" (edit-menu)))

