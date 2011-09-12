(ns llama.statics
  (:use [clojure.java.io :only [file]])
  (:require [seesaw.core :as ssw]))

(def llama-dir (file ".llama"))
(if-not (.exists llama-dir)
  (.mkdirs llama-dir))

(def frame (ssw/frame))
