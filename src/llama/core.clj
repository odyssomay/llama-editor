(ns llama.core
  (:use clj-arrow.arrow
        [clojure.java.io :only [file]])
  (:require (seesaw [core :as ssw]
                    [invoke :as ssw-invoke])
            (llama [editor :as editor]
                   [repl :as repl]
                   [project :as project]
                   [state :as state]))
  (:import (javax.swing.text DefaultEditorKit$CutAction 
                             DefaultEditorKit$CopyAction 
                             DefaultEditorKit$PasteAction)
           (javax.swing JMenuItem)))

(def file-menu-content 
  [(ssw/action :name "New" :tip "Create a new file" :mnemonic \n :key "menu N"
               :handler editor/new-file)
   (ssw/action :name "Open" :tip "Open an existing file" :mnemonic \O :key "menu O"
               :handler editor/open-and-choose-file)
   :separator
   (ssw/action :name "Save" :mnemonic \S :key "menu S"
               :handler editor/save)
   (ssw/action :name "Save As" :mnemonic \A :key "menu shift S"
               :handler editor/save-as)
   :separator
   (ssw/action :name "Close" :tip "Close the current tab" :mnemonic \C :key "menu W"
               :handler editor/remove-current-tab)])

(def edit-menu-content
  [(doto (JMenuItem. (DefaultEditorKit$CutAction.))
     (.setText "Cut")
     (.setAccelerator (seesaw.keystroke/keystroke "menu X")))
   (doto (JMenuItem. (DefaultEditorKit$CopyAction.))
     (.setText "Copy")
     (.setAccelerator (seesaw.keystroke/keystroke "menu C")))
   (doto (JMenuItem. (DefaultEditorKit$PasteAction.))
     (.setText "Paste")
     (.setAccelerator (seesaw.keystroke/keystroke "menu V")))
   :separator
   (ssw/action :name "Undo" :key "menu Z" :handler editor/undo)
   (ssw/action :name "Redo" :key "menu R" :handler editor/redo)])

(def code-menu-content
  [(ssw/action :name "Reconstruct ns" :handler editor/reconstruct-ns)
   (ssw/action :name "Insert proxy" :handler editor/insert-proxy)])

(def repl-menu-content
  [(ssw/action :name "New" :mnemonic \N
               :handler repl/create-new-anonymous-repl)])

(def project-menu-content
  [(ssw/action :name "New" :mnemonic \N :key "menu shift N"
               :handler project/create-and-load-new-project)
   (ssw/action :name "Open" :mnemonic \O :key "menu shift O"
               :handler project/load-project-from-file)])

(defn llama-editor []
  (ssw/native!)
  (let [menubar (ssw/menubar :items [(ssw/menu :text "File" :items file-menu-content)
                                     (ssw/menu :text "Edit" :items edit-menu-content)
                                     (ssw/menu :text "Code" :items code-menu-content)
                                     ;(ssw/menu :text "Repl" :items repl-menu-content)
                                     (ssw/menu :text "Project" :items project-menu-content)])
        p1 (ssw/left-right-split (ssw/scrollable project/project-pane)
                                    editor/editor-pane)
        p2 (ssw/top-bottom-split p1 repl/repl-pane)
        f (ssw/frame :content p2
                     :title "llama editor" 
                     :size [800 :by 500]
                     :menubar menubar)]

    (state/defstate :panel1 #(.getDividerLocation p1))
    (state/defstate :panel2 #(.getDividerLocation p2))
    (state/load-state :panel1 #(.setDividerLocation p1 %))
    (state/load-state :panel2 #(.setDividerLocation p2 %))

    (state/defstate :frame
      (fn []
        [:size [(.getWidth f) :by (.getHeight f)]]))

    (state/load-state :frame
      (fn [state]
        (apply ssw/config! f state)))

    (state/defbean :frame-location #(.getLocation f))
    (state/load-bean :frame-location #(.setLocation f %))

    (ssw/listen 
      f :window-closing 
      (fn [_]  
        (state/save-states)
        (state/save-bean-states)
        (System/exit 0)))
    (ssw/show! f)
    nil))

