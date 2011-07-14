(ns llama.core
  (:gen-class)
  (:use clj-arrow.arrow
        (Hafni.swing action component menu view)
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
  [(comp-and-events (menu-item :text "New" :desc "Create a new file" :mnemonic \n :accelerator "ctrl N")
                    :act editor/new-file)
   (comp-and-events (menu-item :text "Open" :desc "Open an existing file" :mnemonic \O :accelerator "ctrl O")
                    :act editor/open-and-choose-file)
   :separator
   (comp-and-events (menu-item :text "Save" :mnemonic \S :accelerator "ctrl S")
                    :act editor/save)
   (comp-and-events (menu-item :text "Save As" :mnemonic \A :accelerator "ctrl shift S")
                    :act editor/save-as)
   :separator
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
   :separator
   (comp-and-events (menu-item :text "Undo" :accelerator "ctrl Z")
                    :act editor/undo)
   (comp-and-events (menu-item :text "Redo" :accelerator "ctrl R")
                    :act editor/redo)])

(def repl-menu-content
  [(comp-and-events (menu-item :text "New" :mnemonic \N)
                    :act repl/create-new-anonymous-repl)])

(def project-menu-content
  [(comp-and-events (menu-item :text "New" :mnemonic \N :accelerator "ctrl shift N")
                    :act project/create-and-load-new-project)
   (comp-and-events (menu-item :text "Open" :mnemonic \O :accelerator "ctrl shift O")
                    :act project/load-project-from-file)
   (ssw/action :name "test" :command \t)])

(defn -main []
  (let [menubar (ssw/menubar :items [(ssw/menu :text "File" :items (map component file-menu-content))
                                     (ssw/menu :text "Edit" :items (map component edit-menu-content))
                                     (ssw/menu :text "Repl" :items (map component repl-menu-content))
                                     (ssw/menu :text "Project" :items (map component project-menu-content))])
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

