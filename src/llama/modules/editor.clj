(ns llama.modules.editor
  (:use clj-arrow.arrow
        (llama.modules
          [document :only [text-model text-delegate]]
          [syntax :only [indent]]
          [code :only [slamhound-text proxy-dialog]])
        (llama 
               [module-utils :only [add-view send-to-module set-module-focus]]
               [config :only [show-options-dialog]]
               [util :only [drop-nth change-i find-i log 
                            new-file-dialog tab-listener
                            add-tab remove-current-tab
                            current-tab selected-index
                            update-current-tab]]
               [state :only [defstate load-state]])
        [clojure.java.io :only [file]])
  (:require (llama [state :as state])
            (seesaw [core :as ssw]
                    [chooser :as ssw-chooser]
                    [mig :as ssw-mig]))
  (:import llama.util.tab-model))

(defn set-changed-indicator [tab-atom tab changed?]
  (let [i (find-i true (map #(= (:path %) (:path tab)) @tab-atom))
        tab (nth @tab-atom i)]
    (if-not (= (:changed? tab) changed?)
      (swap! tab-atom (fn [tabs] (change-i i #(assoc % :changed? changed?) tabs))))))

(defn open-file [tab-atom file]
  (try 
    (let [file (if (map? file) file 
                 {:title (.getName file) 
                  :path (.getCanonicalPath file)})]
      (if-let [i (find-i (:path file) (map :path @tab-atom))]
        (send-to-module :editor :show i)
        (let [tab (merge file
                         {:changed? false
                          :model (text-model file)})]
          (.addDocumentListener 
            (:model tab)
            (reify javax.swing.event.DocumentListener
              (changedUpdate [_ e] )
              (insertUpdate [_ e]
                (set-changed-indicator tab-atom tab true))
              (removeUpdate [_ e] 
                (set-changed-indicator tab-atom tab true))))
          (swap! tab-atom conj tab))))
    (catch Exception e
      (log :error e (str "failed to open file")))))

(defn open-and-choose-file [tab-atom]
  (if-let [f (ssw-chooser/choose-file)]
    (let [file-map {:title (.getName f)
                    :path (.getCanonicalPath f)}]
      (open-file tab-atom file-map))))

(defn new-file [tmodel] 
  (let [tab {:title "Untitled" :path nil}]
    (open-file (:tabs tmodel) tab)))

(defn save-file [tmodel path]
  (try 
    (let [tab (current-tab tmodel)
          d (:model tab)
          text (.getText d 0 (.getLength d))]
      (spit path text)
      (send-to-module :editor :saved!)
      (set-changed-indicator (:tabs tmodel) tab false))
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

(def find-replace-frame
  (memoize
    (fn []
      (let [find-text (ssw/text :columns 20 :border 10)
            replace-text (ssw/text :columns 20 :border 10)
            match-case (ssw/checkbox :text "case sensitive")
            whole-word (ssw/checkbox :text "separate word")
            panel
            (ssw-mig/mig-panel :items
              [["find" "wrap"] [find-text "span"]
               ["replace" "wrap"] [replace-text "span"]
               [match-case] [whole-word "wrap"]
               [(ssw/action :name "find"
                  :handler (fn [_] 
                             (if-not (empty? (.getText find-text))
                               (send-to-module :editor :find-replace 
                                               (.getText find-text)
                                               (.isSelected match-case) 
                                               (.isSelected whole-word)
                                               true))))]
               [(ssw/action :name "replace"
                  :handler (fn [_]
                             (if-not (empty? (.getText find-text))
                               (send-to-module :editor :find-replace 
                                               (.getText find-text) (.getText replace-text)
                                               (.isSelected match-case) (.isSelected whole-word)
                                               true))))]])
            f (ssw/frame :title "Find/Replace" :content panel)]
        f))))

(defn find-replace 
  ([] (-> (find-replace-frame) ssw/pack! ssw/show!))
  ([area find-text case? separate? regex?]
   (ssw/listen (find-replace-frame) :window-closing
     (fn [_] (.clearMarkAllHighlights area)))
   (.markAll area find-text case? separate? regex?))
  ([area find-text replace-text case? separate? regex?]
   (org.fife.ui.rtextarea.SearchEngine/replaceAll
     area find-text replace-text
     case? separate? regex?)))

;; states

(def current-tabs (atom []))

(defn editor-view []
  (let [tabs-atom current-tabs
        tp (ssw/tabbed-panel :overflow :scroll)
        tmodel (tab-model. tp tabs-atom)
        current-text-area (fn []
                            (.getTextArea (.getSelectedComponent tp)))
        marked (atom [])
        action-fn
        (fn [id & vs]
          (case id
            :new            (new-file tmodel)
            :save           (save tmodel)
            :save-as        (save-as tmodel)
            :remove-current-tab 
                            (remove-current-tab tmodel)
            :undo           (.undoLastAction (current-text-area))
            :redo           (.redoLastAction (current-text-area))
            :indent         (indent-selection (current-text-area))
            :indent-right   (change-indent (current-text-area) :right)
            :indent-left    (change-indent (current-text-area) :left)
            :find-replace   (if vs 
                              (apply find-replace (current-text-area) vs)
                              (find-replace))
            (log :error (str "action not supported by editor: " id))))]
    (let [update-f (memoize
                     (fn [raw-tab]
                       (let [tab (text-delegate raw-tab)]
                         (ssw/listen (:text-pane tab) :focus-gained
                           (fn [_] (set-module-focus :editor action-fn)))
                         tab)))
          listener (tab-listener tmodel #(= (map :path %1) (map :path %2))  
                     (comp (fn [tab]
                             (if (:changed? tab)
                               (update-in tab [:title] str "*")
                               tab)) 
                           update-f))]
      (add-watch tabs-atom (gensym) listener)
      (listener nil nil [] @tabs-atom))
    (set-module-focus :editor action-fn)
    {:content tp}))

(defn init-module []
  (defstate :editor-tabs #(map :path @current-tabs))
  (load-state :editor-tabs
    (fn [paths]
      (doseq [path paths]
        (let [f (file path)]
          (open-file current-tabs
                     {:path (.getCanonicalPath f)
                      :title (.getName f)})))))
  (add-view "editor" editor-view))

