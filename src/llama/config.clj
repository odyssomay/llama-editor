(ns llama.config
  (:use [llama 
         [util :only [log *available-fonts* font find-i]]
         [state :only [defstate load-state]]])
  (:require [seesaw 
             [core :as ssw]
             [graphics :as ssw-graphics]
             [mig :as ssw-mig]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; options datastructure

(def options (atom {}))
(defstate :options #(deref options))

(defn get-option
  ([class-id]
   (get @options class-id))
  ([class-id id]
   (get-in @options [class-id id])))

(defn set-option [class-id id value]
  (swap! options assoc-in [class-id id] value))

(defn listen-to-option
  ([class-id f]
   (if-let [v (get-option class-id)]
     (f nil v))
   (add-watch options (gensym) 
     (fn [_ _ old-item item]
       (let [o (get old-item class-id)
             n (get item class-id)]
         (if-not (= o n)
           (f o n))))))
  ([class-id id f]
   (listen-to-option class-id
     (fn [old-item item]
       (let [o (get old-item id)
             n (get item id)]
         (if-not (= o n)
           (f o n)))))))

(defn init-options []
  (let [saved (load-state :options)]
    (reset! options
      (if saved saved
        {:general
         {:native-look? false}
         :color
         {:background [26 26 26]
          :text [0 0 0]}
         :editor 
         {:wrap? false
          :numbering? true
          :highlight-line? false
          :monospaced? true
          :font (first (filter #(case %
                                  "Lucida Console" true
                                  "Liberation Mono" true
                                  "Courier" true
                                  "Courier new" true
                                  "Monospaced" true
                                  nil) *available-fonts*))
          :font-size 11}}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utils

(defn checkbox [text class-id id]
  (let [c (ssw/checkbox :text text :selected? (get-option class-id id))]
    (ssw/listen c :selection
      (fn [& _] (set-option class-id id (.isSelected c))))
    c))

(defn combobox [choices class-id id]
  (let [cb (ssw/combobox :model choices)]
    (.setSelectedItem cb (get-option class-id id))
    (ssw/listen cb :selection
      (fn [& _] (set-option class-id id (.getSelectedItem cb))))
    cb))

(defn number-spinner [init-val minimum maximum class-id id]
  (let [m (javax.swing.SpinnerNumberModel. init-val minimum maximum 1)
        s (javax.swing.JSpinner. m)]
    (-> s
      .getEditor
      .getTextField
      (.setColumns 7))
    (.setValue m (get-option class-id id))
    (.addChangeListener m
      (reify javax.swing.event.ChangeListener
        (stateChanged [_ _]
          (set-option class-id id (.getValue m)))))
    s))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; general

(defn general-options []
  (let [p (ssw-mig/mig-panel :items [[(checkbox "use native look" :general :native-look?)]])]
    p))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; colors

(let [components (atom [])]
  (defn listen-to-ui-update [component]
    (swap! components conj component))

  (defn fire-ui-update []
    (doseq [c @components]
      (javax.swing.SwingUtilities/updateComponentTreeUI c))))

(defn color-preview-component [id & keys]
  (let [arc-width 5
        arc-height 5
        width 70
        height 20
        inset 2
        cc (javax.swing.JColorChooser.)
        component
        (proxy [javax.swing.JComponent] []
          (paintComponent [g]
            (let [gc (.create g)
                  b (.getClipBounds g)]
              (ssw-graphics/anti-alias gc)
              ;(.setColor gc (java.awt.Color/black))
              (.setColor gc (.getColor cc))
              (.fillRoundRect gc 0 0 width height arc-width arc-height)
              ;(.fillRoundRect gc (.x b) (.y b) (.width b) (.height b) arc-width arc-height)
              ;(.setColor gc (.getColor cc))
              ;(.fillRoundRect gc inset inset (- width inset) (- height inset) arc-width arc-height)
              ;(.fillRoundRect gc (+ (.x b) inset) (+ (.y b) inset) (- (.width b) inset) (- (.height b) inset) arc-width arc-height)
              ;(.drawRoundRect gc 0 0 (.width b) (.height b) arc-width arc-height)
              ))
          (getPreferredSize []
            (java.awt.Dimension. width height))
          (getMinimumSize [] (.getPreferredSize this))
          (getMaximumSize [] (.getPreferredSize this))
          )
        frame (ssw/frame :content cc)]
    (.addChangeListener (.getSelectionModel cc)
      (reify javax.swing.event.ChangeListener
        (stateChanged [_ _]
          (let [color (.getColor cc)]
            (set-option :color id [(.getRed color) (.getGreen color) (.getBlue color)])
            ;(doseq [k keys]
            ;  (javax.swing.UIManager/put k color))
            (.repaint component)
            ;(.repaint cc)
            ))))
    (if-let [[r g b] (get-option :color id)]
      (.setColor cc (java.awt.Color. r g b)))
    (ssw/listen component :mouse-clicked
      (fn [& _] (-> frame ssw/pack! ssw/show!)))
    (listen-to-ui-update cc)
    component))

(defn generate-color-options [& options]
  (apply concat
    (for [[id & keys] options]
      [[(name id)] [(javax.swing.Box/createHorizontalGlue) "pushx"]
       [(apply color-preview-component id keys) "grow 0, wrap"]])))

(defn color-options []
  (ssw-mig/mig-panel :constraints ["fillx" "" ""]
    :items (generate-color-options 
             [:background "control"]
             [:text "text"])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; editor

(defn is-monospaced? [font-name]
  (let [fm (.getFontMetrics (ssw/label) (java.awt.Font. font-name java.awt.Font/PLAIN 10))]
    (= (.charWidth fm \M) (.charWidth fm \i) (.charWidth fm \.))))

(defn editor-options []
  (ssw-mig/mig-panel :constraints ["fillx" "" ""]
    :items [["View"]                                  
              ["Font" "wrap"]
            [(checkbox "text wrapping" :editor :wrap?)] 
              [(checkbox "monospaced" :editor :monospaced?) "wrap"]
            [(checkbox "line numbering" :editor :numbering?)] 
              [(let [cb (combobox *available-fonts* :editor :font)]
                 (listen-to-option :editor :monospaced?
                                   (fn [_ item]
                                     (let [selected (.getSelectedItem cb)]
                                       (ssw/config! cb :model
                                         (filter (if item is-monospaced? identity) *available-fonts*))
                                       (.setSelectedItem cb selected))))
                 cb) "wrap"]
            [(checkbox "highlight current line" :editor :highlight-line?)] 
              ["size" "split 2"] [(number-spinner 10 1 20 :editor :font-size) "wrap"]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; options UI

(defn options-panel []
  (let [tp (ssw/tabbed-panel 
             :tabs [{:title "general" :content (general-options)}
                    {:title "color" :content (color-options)}
                    {:title "editor" :content (editor-options)}])]
    (listen-to-ui-update tp)
    tp))

(def options-dialog
  (memoize 
    #(ssw/frame :content (options-panel) :size [700 :by 300] :title "Preferences")))

;(listen-to-option :color (fn [_ _] (.updateUI (options-panel))))

(defn show-options-dialog []
  (-> (options-dialog) ssw/show!))
