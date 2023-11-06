(ns status-im.ui.screens.chat.sheets
  (:require
    [re-frame.core :as re-frame]))

(defn hide-sheet-and-dispatch
  [event]
  (re-frame/dispatch [:bottom-sheet/hide-old])
  (re-frame/dispatch event))
