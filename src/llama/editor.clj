(ns llama.editor
    (:use clj-arrow.arrow
          (llama document lib
                 [syntax :only [indent]]
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
      (let [i (.getSelectedIndex tabbed_pane)]
        (.removeAll tp)
        (ssw/config! tp :tabs items); (map #(assoc :tip % (:path %)) items))
        (let [i (max 0 (min i (dec (.getTabCount tabbed_pane))))]
          (.setSelectedIndex tabbed_pane i)
          (.requestFocusInWindow (:text-pane (nth @docs i)))))))

  (defn selected-index [& _]
    (let [i (.getSelectedIndex tabbed_pane)]
      (if-not (== i -1)
        i)))

  (defn current-tab [& _]
    (nth @docs (selected-index) nil))

  (defn current-component [& _]
    (:content (current-tab)))

  (defn current-text [& _]
    (if-let [tab (current-tab)]
      (.getText (:text-pane tab))))

  (defn set-save-indicator-changed! [& _]
    (let [i (.getSelectedIndex tabbed_pane)
          c (current-component)]
      (when (= (.getClientProperty c :save-indicator) :saved)
        (.putClientProperty c :save-indicator :changed)
        (swap! docs (fn [coll]
                      (change-i i #(assoc % :title (str (:title %) "*")) coll))))))

  (defn set-save-indicator-saved! [& _]
    (let [i (.getSelectedIndex tabbed_pane)
          c (current-component)]
      (when (= (.getClientProperty c :save-indicator) :changed)
        (.putClientProperty c :save-indicator :saved)
        (swap! docs (fn [coll]
                      (change-i i #(assoc % :title (apply str (butlast (:title %)))) coll))))))

  ;; argument is a map with :title :path
  (defn open-file [file]
    (try 
      (if-let [i (find-i (:path file) (map :path @docs))]
        (.setSelectedIndex tabbed_pane i)
        (let [area (create-text-area file)]
          (.putClientProperty (:content area) :save-indicator :saved)
          (.addDocumentListener 
            (.getDocument (:text-pane area))
            (reify javax.swing.event.DocumentListener
              (changedUpdate [_ e] (set-save-indicator-changed!))
              (insertUpdate [_ e] )
              (removeUpdate [_ e] )))
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
  (defn save-file [path]
    (try 
      (let [text (current-text)]
        (spit path text))
      (set-save-indicator-saved!)
      (catch Exception e
        (log :error e (str "failed to save file: " path)))))

  (defn save-as [& _]
    (try 
      (if-let [i (selected-index)]
        (when-let [f (new-file-dialog tabbed_pane)]
          (let [path (.getCanonicalPath f)]
            (save-file path)
            (swap! docs 
                   (fn [coll]
                     (change-i i #(assoc % :path path :title (.getName f)) coll))))))
      (catch Exception e
        (log :error e "failed to save file as"))))

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

  (defn line-indent [line & [toggle?]]
    (let [area (:text-pane (current-tab))
          document (.getDocument area)
          offset (.getLineStartOffset area line)
          text (.getText document 0 offset)
          current-indent  (count (take-while (partial = \space) (subs (.getText area) offset)))
          new-indent (subs (indent text) 1)]
      (.remove document offset current-indent)
      (.insertString document offset (if (and toggle? (== current-indent (count new-indent)))
                                         (subs (indent text true) 1)
                                         new-indent)
                                     nil)))

  (defn indent-selection [& _]
    (let [area (:text-pane (current-tab))
          start-line (.getLineOfOffset area (.getSelectionStart area))
          end-line (.getLineOfOffset area (.getSelectionEnd area))]
      (if (== start-line end-line)
        (line-indent start-line true)
        (doseq [line (range start-line (inc end-line))]
          (line-indent line)))))

  (defn change-indent-right [area line]
    (.insertString (.getDocument area)
                   (.getLineStartOffset area line) "  " nil))

  (defn change-indent-left [area line]
    (let [offset (.getLineStartOffset area line)
          document (.getDocument area)]
      (dotimes [_ 2]
        (if (= (.getText document offset 1) " ")
          (.remove document offset 1)))))

  (defn change-indent [direction]
    (let [area (:text-pane (current-tab))
          start-line (.getLineOfOffset area (.getSelectionStart area))
          end-line (.getLineOfOffset area (.getSelectionEnd area))]
      (doseq [line (range start-line (inc end-line))]
        (case direction
          :right (change-indent-right area line)
          :left (change-indent-left area line))))) 

  (def editor-pane 
    tabbed_pane)

;; states

  (state/defstate :editor-pane
    (fn [] (map #(hash-map :path (:path %)) @docs)))

  (doseq [{p :path} (state/load-state :editor-pane)]
    (open-file {:path p 
                :title (.getName (file p))}))
)

(log :trace "finished loading")
