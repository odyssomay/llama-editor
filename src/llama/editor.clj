(ns llama.editor
  (:use clj-arrow.arrow
        (llama [document :only [text-model text-delegate]]
               [config :only [show-options-dialog]]
               [util :only [drop-nth change-i find-i log 
                            new-file-dialog tab-listener
                            add-tab remove-current-tab
                            current-tab selected-index
                            update-current-tab set-focus]]
               [syntax :only [indent]]
               [code :only [slamhound-text proxy-dialog]]
               [state :only [defstate load-state]])
        [clojure.java.io :only [file]])
    (:require (llama [state :as state])
              (seesaw [core :as ssw]
                      [chooser :as ssw-chooser]))
  (:import llama.util.tab-model))

(defn set-save-indicator-changed [tmodel]
  (let [tab (current-tab tmodel)]
    (when (= (:save-indicator tab) :saved)
      (update-current-tab tmodel #(assoc % :save-indicator :changed))
      (update-current-tab tmodel #(assoc % :title (str (:title %) "*"))))))

(defn set-save-indicator-saved [tmodel]
  (let [tab (current-tab tmodel)]
    (when (= (:save-indicator tab) :changed)
      (update-current-tab tmodel #(assoc % :save-indicator :saved))
      (update-current-tab tmodel #(assoc % :title (apply str (butlast (:title %))))))))

(defn open-file [tmodel file]
  (try 
    (let [file (if (map? file) file 
                 {:title (.getName file) 
                  :path (.getCanonicalPath file)})]
      (if-let [i (find-i (:path file) (map :path @(:tabs tmodel)))]
        (.setSelectedIndex (:tp tmodel) i)
        (let [tab (merge (text-delegate file) 
                         {:save-indicator :saved
                          :model (text-model file)})]
          (.addDocumentListener 
            (:model tab)
            (reify javax.swing.event.DocumentListener
              (changedUpdate [_ e] (set-save-indicator-changed tmodel))
              (insertUpdate [_ e] )
              (removeUpdate [_ e] )))
          (add-tab tmodel tab))))
    (catch Exception e
      (log :error e (str "failed to open file")))))

(defn open-and-choose-file [tmodel]
  (if-let [f (ssw-chooser/choose-file)]
    (let [tab {:title (.getName f)
               :path (.getCanonicalPath f)}]
      (open-file tmodel tab))))

(defn new-file [tmodel] 
  (let [tab {:title "Untitled" :path nil}]
    (open-file tmodel tab)))

(defn save-file [tmodel path]
  (try 
    (let [d (:model (current-tab tmodel))
          text (.getText d 0 (.getLength d))]
        (spit path text)
        (set-save-indicator-saved tmodel))
    (catch Exception e
      (log :error e (str "failed to save file: " path)))))

(defn save-as [tmodel]
  (try 
    (if-let [i (selected-index tmodel)]
      (when-let [f (new-file-dialog (:tp tmodel))]
        (let [path (.getCanonicalPath f)]
          (save-file tmodel path)
          (update-current-tab tmodel
            #(assoc % :path path :title (.getName f))))))
    (catch Exception e
      (log :error e "failed to save file as"))))

(defn save [tmodel]
  (let [tab (current-tab tmodel)]
    (if (:path tab)
      (save-file tmodel (:path tab))
      (save-as tmodel))))

(defn undo [tmodel]
  (let [m (:manager (current-tab tmodel))]
    (if (.canUndo m)
      (.undo m))))

(defn redo [tmodel]
  (let [m (:manager (current-tab tmodel))]
    (if (.canRedo m)
      (.redo m))))

(defn line-indent [area line & [toggle?]]
  (let [document (.getDocument area)
        offset (.getLineStartOffset area line)
        text (.getText document 0 offset)
        current-indent  (count (take-while (partial = \space) (subs (.getText area) offset)))
        new-indent (subs (indent text) 1)]
    (.remove document offset current-indent)
    (.insertString document offset (if (and toggle? (== current-indent (count new-indent)))
                                       (subs (indent text true) 1)
                                       new-indent)
                                   nil)))

(defn indent-selection [area]
  (let [start-line (.getLineOfOffset area (.getSelectionStart area))
        end-line (.getLineOfOffset area (.getSelectionEnd area))]
    (if (== start-line end-line)
      (line-indent area start-line true)
      (doseq [line (range start-line (inc end-line))]
        (line-indent area line)))))

(defn change-indent-right [area line]
  (.insertString (.getDocument area)
                 (.getLineStartOffset area line) "  " nil))

(defn change-indent-left [area line]
  (let [offset (.getLineStartOffset area line)
        document (.getDocument area)]
    (dotimes [_ 2]
      (if (= (.getText document offset 1) " ")
        (.remove document offset 1)))))

(defn change-indent [area direction]
  (let [start-line (.getLineOfOffset area (.getSelectionStart area))
        end-line (.getLineOfOffset area (.getSelectionEnd area))]
    (doseq [line (range start-line (inc end-line))]
      (case direction
        :right (change-indent-right area line)
        :left (change-indent-left area line))))) 

;; states

(def current-tabs (atom []))
(defstate :editor-tabs
  #(map :path @current-tabs))
(load-state :editor-tabs
  (fn [paths]
    (reset! current-tabs 
            (for [path paths]
              (let [f (file path)]
                {:path (.getCanonicalPath f)
                 :title (.getName f)})))))

(defn editor-view []
  (let [tabs-atom current-tabs
        tp (ssw/tabbed-panel :overflow :scroll)
        tmodel (tab-model. tp tabs-atom)
        current-text-area (fn []
                            (.getTextArea (.getSelectedComponent tp)))
        action-fn
        (fn [& [id v]]
          (case id
            :new            (new-file tmodel)
            :open           (if v 
                              (open-file tmodel v)
                              (open-and-choose-file tmodel))
            :save           (save tmodel)
            :save-as        (save-as tmodel)
            :remove-current-tab 
                            (remove-current-tab tmodel)
            :undo           (undo tmodel)
            :redo           (redo tmodel)
            :indent         (indent-selection (current-text-area))
            :indent-right   (change-indent (current-text-area :right))
            :indent-left    (change-indent (current-text-area :left))
            (log :error (str "action not supported by editor: " id))))]
    (let [listener (tab-listener tmodel 
                     (fn [raw-tab]
                       (let [tab (text-delegate raw-tab)]
                         (ssw/listen (:text-pane tab) :focus-gained
                           (fn [_] (set-focus :editor action-fn)))
                         tab)))]
      (add-watch tabs-atom (gensym) listener)
      (listener nil nil [] @tabs-atom))
    {:content tp}
    ))
