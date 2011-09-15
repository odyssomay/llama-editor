(ns llama.module-utils
  (:use [llama.util :only [log]])
  (:require [seesaw.core :as ssw]))

(let [views (atom {})]
  (defn add-view [id f]
    (swap! views assoc id f))

  (defn get-views []
    @views)

  (defn reset-views []
    (reset! views {})))

(let [menus (atom [])]
  (defn add-menu [id m]
    (swap! menus conj [id m]))

  (defn get-menus []
    (map (fn [[id items]]
           (ssw/menu :text id :items items))
         @menus))
  
  (defn reset-menus []
    (reset! menus [])))

(defn add-config [id c] )

(let [focus (atom {})]
  (defn set-module-focus [id f]
    (swap! focus assoc id f))

  (defn send-to-module [id & vs]
    (log :trace (str "sending to module: " id " values: " vs))
    (if-let [f (get @focus id)]
      (apply f vs))))

