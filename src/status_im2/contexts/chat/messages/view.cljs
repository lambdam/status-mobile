(ns status-im2.contexts.chat.messages.view
  (:require
    [react-native.core :as rn]
    [react-native.reanimated :as reanimated]
    [react-native.safe-area :as safe-area]
    [reagent.core :as reagent]
    [status-im2.contexts.chat.composer.view :as composer.view]
    [status-im2.contexts.chat.messages.list.style :as style]
    [status-im2.contexts.chat.messages.list.view :as list.view]
    [status-im2.contexts.chat.messages.navigation.view :as messages.navigation]))

(defn f-chat
  []
  (let [insets (safe-area/get-insets)
        show-floating-scroll-down-button? (reagent/atom false)
        inner-state-atoms
        {:extra-keyboard-height             (reagent/atom 0)
         :show-floating-scroll-down-button? show-floating-scroll-down-button?
         :messages-view-height              (reagent/atom 0)
         :messages-view-header-height       (reagent/atom 0)
         :animate-topbar-name?              (reagent/atom false)
         :big-name-visible?                 (reagent/atom :initial-render)
         :animate-topbar-opacity?           (reagent/atom false)
         :on-end-reached?                   (reagent/atom false)}
        scroll-y (reanimated/use-shared-value 0)]
    ;; Note - Don't pass `behavior :height` to keyboard avoiding view,. It breaks composer -
    ;; https://github.com/status-im/status-mobile/issues/16595
    [rn/keyboard-avoiding-view
     {:style                    (style/keyboard-avoiding-container insets)
      :keyboard-vertical-offset (- (:bottom insets))}

     [list.view/message-list-content-view
      {:insets            insets
       :scroll-y          scroll-y
       :cover-bg-color    :turquoise
       :inner-state-atoms inner-state-atoms}]

     [messages.navigation/navigation-view
      {:scroll-y          scroll-y
       :inner-state-atoms inner-state-atoms}]

     [composer.view/composer
      {:insets                            insets
       :scroll-to-bottom-fn               list.view/scroll-to-bottom
       :show-floating-scroll-down-button? show-floating-scroll-down-button?}]]))

(defn chat
  []
  [:f> f-chat])
