(ns llama.editor
    (:use clj-arrow.arrow
          (llama document lib))
    (:require [clojure.set :as cset] 
              [Hafni.swing.dialog :as file]
              [seesaw.core :as ssw]
              [hafni-seesaw.core :as hssw]))

(let [tp (ssw/tabbed-panel :overflow :scroll)
      tabbed_pane tp
      docs (atom [])]

  (def selected-index
    (ignore #(.getSelectedIndex tabbed_pane)))

  (def current-file
    (>>> selected-index #(nth @docs %)))

  (def current-component
    (>>> selected-index #(:content (nth @docs %))))

  (def current-pane
    current-component)
;    (>>> current-component component))

  (def current-text
    (>>> current-pane #(.getText %)))

  ;; argument is a map with :title :path
  (def open-file
    (>>> clone
         (||| #(find-i (:path %) (map :path @docs))
              (>>> first #(.setSelectedIndex tabbed_pane %))
              (snd (>>> create-doc #(swap! docs conj %) (hssw/input-arr tp :content))))))

  ;; argument is ignored
  (def open-and-choose-file
    (>>> (constantly [nil nil])
         (||| (ignore file/open-file)
              (>>> first #(cset/rename-keys % {:name :title}) open-file))))

  ;; argument is ignored
  (def new-file 
    (>>> (constantly {:title "Untitled" :path nil})
         open-file))

  ;; argument is a pair where the first value is the path
  ;; and the second value is ignored
  (def save-file
    (>>> (snd current-text) (fn [[path text]] (spit path text))))

  ;; argument is ignored
  (def save-as
    (>>> (constantly [nil nil])
         (||| (ignore #(:path (file/save-file)))
              (&&& save-file
                   (>>> (snd selected-index)
                        (fn [[path index]] 
                          (swap! docs (fn [coll] 
                                        (change-i index #(assoc % :title path) coll))))
                        (hssw/input-arr tp :content))))))

  ;; argument is ignored
  (def save
    (>>> current-file
         (>>> clone
              (||| :path
                   save-file
                   save-as))))

  (def undo
    (>>> current-file
         (>>> :manager
              #(if (.canUndo %) (.undo %)))))

  (def redo
    (>>> current-file
         (>>> :manager
              #(if (.canRedo %) (.redo %)))))

  ;;argument is ignored
  (def remove-current-tab
    (>>> selected-index (fn [n] (swap! docs #(drop-n % n))) (hssw/input-arr tp :content)))

  (def editor-pane 
    tabbed_pane))


