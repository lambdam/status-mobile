(ns status-im2.contexts.onboarding.generating-keys.style
  (:require [quo2.foundations.colors :as colors]))

(def default-margin 20)

(defn page-container
  [insets]
  {:flex             1
   :justify-content  :space-between
   :padding-top      (:top insets)
   :background-color colors/neutral-80-opa-80-blur})
