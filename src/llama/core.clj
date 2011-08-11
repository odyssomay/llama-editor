(use '[seesaw.core :only [native!]])
(native!)

(ns llama.core
  (:use clj-arrow.arrow
        [clojure.java.io :only [file]])
  (:require (seesaw [core :as ssw]
                    [invoke :as ssw-invoke])
            (llama [editor :as editor]
                   [repl :as repl]
                   [project :as project]
                   [state :as state]
                   [lib :as lib]
                   [statics :as statics]))
  (:import java.awt.event.KeyEvent
           (javax.swing.text DefaultEditorKit$CutAction 
                             DefaultEditorKit$CopyAction 
                             DefaultEditorKit$PasteAction)
           (javax.swing JMenuItem)))

(lib/log :trace "started loading")

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
  [(ssw/menu-item :action (DefaultEditorKit$CutAction.) :text "Cut" :key "menu X")
   (ssw/menu-item :action (DefaultEditorKit$CopyAction.) :text "Copy" :key "menu C")
   (ssw/menu-item :action (DefaultEditorKit$PasteAction.) :text "Paste" :key "menu V")
   :separator
   (ssw/action :name "Undo" :key "menu Z" :handler editor/undo)
   (ssw/action :name "Redo" :key "menu R" :handler editor/redo)
   :separator
   (ssw/action :name "Indent" :key "menu I" :handler editor/indent-selection)
   (let [a (ssw/action :name "Indent right" :handler (fn [_] (editor/change-indent :right)))]
     (.putValue a javax.swing.Action/ACCELERATOR_KEY 
                (javax.swing.KeyStroke/getKeyStroke KeyEvent/VK_RIGHT KeyEvent/ALT_DOWN_MASK))
     a)
   (let [a (ssw/action :name "Indent left"  :handler (fn [_] (editor/change-indent :left)))]
     (.putValue a javax.swing.Action/ACCELERATOR_KEY
                (javax.swing.KeyStroke/getKeyStroke KeyEvent/VK_LEFT KeyEvent/ALT_DOWN_MASK))
     a)])

(def code-menu-content
  [(ssw/action :name "Reconstruct ns" :handler editor/reconstruct-ns)
   ;(ssw/action :name "Insert proxy" :handler editor/insert-proxy)
   ])

(def repl-menu-content
  [(ssw/action :name "New" :mnemonic \N
               :handler repl/create-new-anonymous-repl)])

(def project-menu-content
  [(ssw/action :name "New" :mnemonic \N :key "menu shift N"
               :handler project/create-and-load-new-project)
   (ssw/action :name "Open" :mnemonic \O :key "menu shift O"
               :handler project/load-project-from-file)])

(defn load-editor []
  (let [menubar (ssw/menubar :items [(ssw/menu :text "File" :items file-menu-content)
                                     (ssw/menu :text "Edit" :items edit-menu-content)
                                     ;(ssw/menu :text "Code" :items code-menu-content)
                                     ;(ssw/menu :text "Repl" :items repl-menu-content)
                                     (ssw/menu :text "Project" :items project-menu-content)])
        p1 (ssw/left-right-split 
             (ssw/scrollable project/project-pane) editor/editor-pane
             :divider-location 1/4)
        p2 (ssw/top-bottom-split p1 repl/repl-pane :divider-location 2/3)
        f statics/frame]

    (ssw/config! f
                 :content p2
                 :title "llama editor" 
                 :size [800 :by 500]
                 :menubar menubar)

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
    (state/load-bean :frame-location #(.setLocation f %))))

(defn llama-editor []
  (load-editor)
  (ssw/listen 
    statics/frame :window-closing 
    (fn [_]  
      (state/save-states)
      (state/save-bean-states)
      (System/exit 0)))
  (ssw/show! statics/frame)
  nil)

(lib/log :trace "finished loading")
