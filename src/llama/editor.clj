(ns llama.editor
    (:use clj-arrow.arrow
          (llama document lib
                 [code :only [slamhound-text proxy-dialog]])
          [clojure.java.io :only [file]])
    (:require (llama [state :as state])
              (seesaw [core :as ssw]
                      [chooser :as ssw-chooser])))

(log :trace "started loading")

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
  (defn open-file [file]
    (try 
      (if-let [i (find-i (:path file) (map :path @docs))]
        (.setSelectedIndex tabbed_pane i)
        (let [area (create-text-area file)]
          (swap! docs conj area)))
      (catch Exception e
        (log :error e (str "failed to open file")))))

  (defn open-and-choose-file [& _]
    (if-let [f (ssw-chooser/choose-file)]
      (open-file {:title (.getName f)
                  :path (.getCanonicalPath f)})))

  ;; argument is ignored
  (def new-file 
    (>>> (constantly {:title "Untitled" :path nil})
         open-file))

  ;; argument is the path
  (def save-file
    (>>> clone (snd current-text) (fn [[path text]] (spit path text))))

  (defn save-as [& _]
    (try 
      (when-let [f (new-file-dialog)]
        (let [path (.getCanonicalPath f)]
          (save-file path)
          (let [i (selected-index)]
            (swap! docs 
                   (fn [coll]
                     (change-i i #(assoc % :path path :title (.getName f)) coll))))))
      (catch Exception e
        (log :error e (str "failed to save file")))))

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

;; states

  (state/defstate :editor-pane
    (fn [] (map #(hash-map :title (:title %)
                           :path (:path %)) @docs)))

  (doseq [d (state/load-state :editor-pane)]
    (open-file d))
)

(log :trace "finished loading")
