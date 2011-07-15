(ns llama.editor
    (:use clj-arrow.arrow
          (llama document lib)
          [clojure.java.io :only [file]])
    (:require (llama [state :as state])
              [Hafni.swing.dialog :as file]
              (seesaw [core :as ssw]
                      [chooser :as ssw-chooser])
              [hafni-seesaw.core :as hssw]))

(let [tp (ssw/tabbed-panel :overflow :scroll)
      tabbed_pane tp
      docs (atom [])] 

  (def selected-index
    (ignore #(.getSelectedIndex tabbed_pane)))

  (def current-tab
    (>>> selected-index #(nth @docs %)))

  (def current-component
    (>>> selected-index #(:content (nth @docs %))))

  (def current-text
    (>>> current-tab :text-pane #(.getText %)))

  ;; argument is a map with :title :path
  (def open-file
    (>>> clone
         (||| #(find-i (:path %) (map :path @docs))
              (>>> first #(.setSelectedIndex tabbed_pane %))
              (snd (>>> create-doc #(swap! docs conj %) (hssw/input-arr tp :content))))))

  ;; argument is ignored
  (def open-and-choose-file
    (>>> (constantly [nil nil])
         (||| (fn [_] 
                (if-let [f (ssw-chooser/choose-file)]
                  {:title (.getName f)
                   :path (.getCanonicalPath f)}))
              (>>> first open-file))))

  ;; argument is ignored
  (def new-file 
    (>>> (constantly {:title "Untitled" :path nil})
         open-file))

  ;; argument is the path
  (def save-file
    (>>> clone (snd current-text) (fn [[path text]] (spit path text))))

  ;; argument is ignored
  (def save-as
    (>>> (constantly [nil nil])
         (||| (ignore #(.getCanonicalPath (ssw-chooser/choose-file)))
              (&&& (fst save-file)
                   (>>> (snd selected-index)
                        (fn [[path index]] 
                          (swap! docs (fn [coll]
                                        (change-i index #(assoc % :path path :title (.getName (file path))) coll))))
                        (hssw/input-arr tp :content))))))

  ;; argument is ignored
  (def save
    (>>> current-tab
         (>>> clone
              (||| :path
                   (fst save-file)
                   save-as))))

  (def undo
    (>>> current-tab
         (>>> :manager
              #(if (.canUndo %) (.undo %)))))

  (def redo
    (>>> current-tab
         (>>> :manager
              #(if (.canRedo %) (.redo %)))))

  ;;argument is ignored
  (def remove-current-tab
    (>>> selected-index (fn [n] (swap! docs #(drop-nth % n))) (hssw/input-arr tp :content)))

  (def editor-pane 
    tabbed_pane)

  (state/defstate :editor-pane
    (fn [] (map #(hash-map :title (:title %)
                           :path (:path %)) @docs)))

  (doseq [d (state/load-state :editor-pane)]
    (open-file d))
)

