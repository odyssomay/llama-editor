(ns llama.editor
    (:use clj-arrow.arrow
          (llama [document :only [text-model text-delegate]]
                 [lib :only [drop-nth change-i find-i log new-file-dialog]]
                 [syntax :only [indent]]
                 [code :only [slamhound-text proxy-dialog]])
          [clojure.java.io :only [file]])
    (:require (llama [state :as state])
              (seesaw [core :as ssw]
                      [chooser :as ssw-chooser]))
  (:import java.awt.event.KeyEvent))

(defprotocol tab-model-p 
  (add-tab [this tab])
  (remove-current-tab [this])
  (current-tab [this])
  (selected-index [this])
  (update-current-tab [this f]))

(defrecord tab-model [tp tabs]
  tab-model-p
  (add-tab [this tab] (swap! tabs conj tab))
  (remove-current-tab [this]
    (when-let [i (selected-index this)]
      (swap! tabs #(drop-nth % i))))
  (current-tab [this]
    (if-let [i (selected-index this)]
      (nth @tabs i nil)))
  (selected-index [this]
    (let [i (.getSelectedIndex tp)]
      (if-not (= i -1)
        i)))
  (update-current-tab [this f]
    (swap! tabs
      (fn [coll]
        (change-i (selected-index this) f coll)))))

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
        (add-tab tmodel tab)))
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

(defn set-tabs [tp raw-items]
  (let [items (map text-delegate raw-items)] 
    (.removeAll tp)
    (doseq [{:keys [content title path]} items]
      (.addTab tp title nil content path)
   )
    ;(ssw/config! tp :tabs items)
    ))

(defn add-tabs-listener [tmodel tab-atom]
  (add-watch tab-atom (gensym)
    (fn [_ _ old-items raw-items]
      (let [tp (:tp tmodel)]
        (if-not (= (count old-items) (count raw-items))
          (set-tabs tp raw-items)
          (doseq [i (range (.getTabCount tp))]
            (when-let [{:keys [path title]} (nth raw-items i nil)]
              (.setTitleAt tp i title)
              (.setToolTipTextAt tp i path))))))))

;        (let [tp (:tp tmodel)
;              items (map text-delegate raw-items)
              ;(for [t raw-items]
                    ;  (if (contains? t :content)
                    ;    t
                    ;    (text-delegate t)))
;              i (selected-index tmodel)
;              selected-c (:content (current-tab tmodel))
;              ]
;          (.removeAll tp)
;          (ssw/config! tp :tabs items)
          ; (map #(assoc :tip % (:path %)) items))
          ;(if (some (partial = selected-c) (map :content items))
          ;  (do (.setSelectedComponent tp selected-c)
          ;      (.requestFocusInWindow selected-c))
            ;(.setSelectedIndex tp (max 0 (min (dec (.getTabCount tp)) i)))
            ;)
;          )))))

(defn editor-view []
  (let [tabs-atom current-tabs
        tp (ssw/tabbed-panel :overflow :scroll)
        tmodel (tab-model. tp tabs-atom)
        current-text-area (fn []
                            (.getTextArea (.getSelectedComponent tp)))
        m (ssw/menubar 
            :items 
            [(ssw/menu 
               :text "file" :items
               [(ssw/action :name "New" :tip "Create a new file" :mnemonic \n :key "menu N"
                            :handler (fn [_] (new-file tmodel)))
                (ssw/action :name "Open" :tip "Open an existing file" :mnemonic \O :key "menu O"
                            :handler (fn [_] (open-and-choose-file tmodel)))
                :separator
                (ssw/action :name "Save" :mnemonic \S :key "menu S"
                            :handler (fn [_] (save tmodel)))
                (ssw/action :name "Save As" :mnemonic \A :key "menu shift S"
                            :handler (fn [_] (save-as tmodel)))
                :separator
                (ssw/action :name "Close" :tip "Close the current tab" :mnemonic \C :key "menu W"
                            :handler (fn [_] (remove-current-tab tmodel)))])
             (ssw/menu
               :text "edit" :items
               [(ssw/menu-item :action (DefaultEditorKit$CutAction.) :text "Cut" :key "menu X")
                (ssw/menu-item :action (DefaultEditorKit$CopyAction.) :text "Copy" :key "menu C")
                (ssw/menu-item :action (DefaultEditorKit$PasteAction.) :text "Paste" :key "menu V")
                :separator
                (ssw/action :name "Undo" :key "menu Z" :handler (fn [_] (undo tmodel)))
                (ssw/action :name "Redo" :key "menu R" :handler (fn [_] (redo tmodel)))
                :separator
                (ssw/action :name "Indent" :key "menu I" 
                            :handler (fn [_] (indent-selection (current-text-area))))
                (let [a (ssw/action :name "Indent right" ;:key "menu shift right"
                                    :handler (fn [_] (change-indent (current-text-area) :right)))]
                  (.putValue a javax.swing.Action/ACCELERATOR_KEY
                             (javax.swing.KeyStroke/getKeyStroke KeyEvent/VK_RIGHT KeyEvent/ALT_DOWN_MASK))
                  a)
                (let [a (ssw/action :name "Indent left"  
                                    :handler (fn [_] (change-indent (current-text-area) :left)))]
                  (.putValue a javax.swing.Action/ACCELERATOR_KEY
                             (javax.swing.KeyStroke/getKeyStroke KeyEvent/VK_LEFT KeyEvent/ALT_DOWN_MASK))
                  a)])])]
    (add-tabs-listener tmodel tabs-atom)
    (set-tabs tp @tabs-atom)
    (swap! tabs-atom identity)
    {:content tp :menu m}))
