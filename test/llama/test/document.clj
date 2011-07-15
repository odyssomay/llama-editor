(ns llama.test.document
  (:use [llama.document]
        [lazytest.describe :only [describe it testing]]))

(describe get-font
  (let [font (get-font)
        fontmetric (-> (java.awt.image.BufferedImage. 1 1 java.awt.image.BufferedImage/TYPE_INT_ARGB)
                       .createGraphics
                       (.getFontMetrics font))]
    (it "should return a font"
        (= (class font) java.awt.Font))
    (it "should be monospaced"
        (= (.charWidth fontmetric \space)
           (.charWidth fontmetric \A)))))

(describe get-styles
  (let [styles (get-styles)]
    (it "should return a vector"
        (vector? styles))
    (testing "the styles"
      (it "should be maps"
          (every? map? styles))
      (it "should contain a :name"
          (every? :name styles)))))

(describe init-undoable-edits
  (let [t (javax.swing.JTextPane.)
        d (.getDocument t)
        manager (init-undoable-edits d)]
    (it "should be an UndoManager"
        (= (class manager) javax.swing.undo.UndoManager))))

(describe in-string?
  (it "should return true if in a string"
      (in-string? "(def a \"bla"))
  (it "should return false if not in a string"
      (not (in-string? "(def a \"blabla\""))))

(describe create-highlight-fn
  (let [t (javax.swing.JTextPane.)]
    (it "should return a function"
        (fn? (create-highlight-fn t)))))

(describe create-doc
  (it "should create a JTextPane"
      (= (class (:text-pane (create-doc {}))) javax.swing.JTextPane))
  (it "should have a DefaultStyledDocument by default"
      (= (class (.getStyledDocument (:text-pane (create-doc {})))) javax.swing.text.DefaultStyledDocument))
  (it "should not have a DefaultStyledDocument for clojure text"
      (not= (class (.getStyledDocument (:text-pane (create-doc {:type "clj"})))) javax.swing.text.DefaultStyledDocument)))
