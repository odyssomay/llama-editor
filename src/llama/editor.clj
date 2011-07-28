(ns llama.editor
    (:use clj-arrow.arrow
          (llama document lib
                 [code :only [slamhound-text proxy-dialog]])
          [clojure.java.io :only [file]])
    (:require (llama [state :as state])
              (seesaw [core :as ssw]
                      [chooser :as ssw-chooser])))

(let [tp (ssw/tabbed-panel :overflow :scroll)
      tabbed_pane tp
      docs (atom [])] 

  (add-watch docs nil
    (fn [_ _ _ items]
      (.removeAll tp)
      (ssw/config! tp :tabs items)))

  (def selected-index
    (ignore #(.getSelectedIndex tabbed_pane)))

  (defn current-tab [& _]
    (nth @docs (selected-index) nil))

  (defn current-component [& _]
    (:content (current-tab)))

  (def current-text
    (>>> current-tab :text-pane #(.getText %)))

  ;; argument is a map with :title :path
  (def open-file
    (>>> clone
         (||| #(find-i (:path %) (map :path @docs))
              (>>> first #(.setSelectedIndex tabbed_pane %))
              (snd (>>> create-text-area #(swap! docs conj %) 
                        ;(hssw/input-arr tp :content)
                        )))))

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
                                        (change-i index #(assoc % :path path :title (.getName (file path))) coll)))))))))

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
    (>>> selected-index (fn [n] (swap! docs #(drop-nth % n)))))

  (defn reconstruct-ns [& _]
    (.start (Thread.
              (fn [] 
                (let [text_pane (:text-pane (current-tab))
                      new_ns (slamhound-text (.getText text_pane))]
                  (ssw/invoke-later 
                    (.insertString (.getDocument text_pane)
                                   0 new_ns nil)))))))

  (defn insert-proxy [& _]
    (let [text_pane (:text-pane (current-tab))
          proxy_text (proxy-dialog)]
      (println "proxy_text:")
;      (ssw/invoke-later
        (.insertString (.getDocument text_pane)
                       (.getCaretPosition text_pane) "" nil)));)

  (def editor-pane 
    tabbed_pane)

  (state/defstate :editor-pane
    (fn [] (map #(hash-map :title (:title %)
                           :path (:path %)) @docs)))

  (doseq [d (state/load-state :editor-pane)]
    (open-file d))
)

