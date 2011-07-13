(ns llama.core
  (:gen-class)
  (:use clj-arrow.arrow
        (Hafni.swing action component menu view))
  (:require [seesaw.core :as ssw]
            (llama [editor :as editor]
                   [repl :as repl]
                   [project :as project]))
  (:import (javax.swing.text DefaultEditorKit$CutAction 
                             DefaultEditorKit$CopyAction 
                             DefaultEditorKit$PasteAction)
           (javax.swing JMenuItem)))

(def file-menu-content 
  [(comp-and-events (menu-item :text "New" :desc "Create a new file" :mnemonic \n :accelerator "ctrl N")
                    :act editor/new-file)
   (comp-and-events (menu-item :text "Open" :desc "Open an existing file" :mnemonic \O :accelerator "ctrl O")
                    :act editor/open-and-choose-file)
   []
   (comp-and-events (menu-item :text "Save" :mnemonic \S :accelerator "ctrl S")
                    :act editor/save)
   (comp-and-events (menu-item :text "Save As" :mnemonic \A :accelerator "ctrl shift S")
                    :act editor/save-as)
   []
   (comp-and-events (menu-item :text "Close" :desc "Close the current tab" :mnemonic \C :accelerator "ctrl W")
                    :act editor/remove-current-tab)])

(def edit-menu-content
  [(doto (JMenuItem. (DefaultEditorKit$CutAction.))
     (.setText "Cut")
     (.setAccelerator (seesaw.keystroke/keystroke "ctrl X")))
   (doto (JMenuItem. (DefaultEditorKit$CopyAction.))
     (.setText "Copy")
     (.setAccelerator (seesaw.keystroke/keystroke "ctrl C")))
   (doto (JMenuItem. (DefaultEditorKit$PasteAction.))
     (.setText "Paste")
     (.setAccelerator (seesaw.keystroke/keystroke "ctrl V")))
   []
   (comp-and-events (menu-item :text "Undo" :accelerator "ctrl Z")
                    :act editor/undo)
   (comp-and-events (menu-item :text "Redo" :accelerator "ctrl R")
                    :act editor/redo)])

(def repl-menu-content
  [(comp-and-events (menu-item :text "New" :mnemonic \N)
                    :act repl/create-new-anonymous-repl)])

(def project-menu-content
  [(comp-and-events (menu-item :text "New" :mnemonic \N :accelerator "ctrl shift N")
                    :act project/create-new-project)
   (comp-and-events (menu-item :text "Open" :mnemonic \O :accelerator "ctrl shift O")
                    :act project/load-project-from-file)])

(defn -main []
  (let [menubar (menu-bar :content [(menu "File" :content file-menu-content)
                                    (menu "Edit" :content edit-menu-content)
                                    (menu "Repl" :content repl-menu-content)
                                    (menu "Project" :content project-menu-content)])]
    (frame :content 
           (ssw/top-bottom-split 
             (ssw/left-right-split (ssw/scrollable project/project-pane)
                                   editor/editor-pane)
                                   repl/repl-pane)
           :title "llama editor" 
           :size 800 500
           :menu_bar menubar)
    nil))

