(ns status-im2.contexts.wallet.add-address-to-watch.view
  (:require
    [clojure.string :as string]
    [quo.core :as quo]
    [react-native.clipboard :as clipboard]
    [react-native.core :as rn]
    [reagent.core :as reagent]
    [status-im2.contexts.wallet.add-address-to-watch.style :as style]
    [utils.i18n :as i18n]
    [utils.re-frame :as rf]))

(defn- address-input
  [{:keys [input-value
           paste-on-input clear-input]}]
  (let [scanned-address (rf/sub [:wallet/scanned-address])
        empty-input?    (and (string/blank? @input-value)
                             (string/blank? scanned-address))
        on-change-text  (fn [new-text]
                          (reset! input-value new-text)
                          (prn "scanned-address" scanned-address)
                          (when (and scanned-address (not= scanned-address new-text))
                            (rf/dispatch [:wallet/clean-scanned-address])))]
    (rn/use-effect (fn []
                     (when-not (string/blank? scanned-address)
                       (on-change-text scanned-address)))
                   [scanned-address])
    [rn/view
     {:style {:flex-direction    :row
              :margin-horizontal 20}}
     [quo/input
      {:placeholder     (i18n/label :t/address-placeholder)
       :container-style {:flex 1 :margin-right 20}
       :label           (i18n/label :t/enter-eth)
       :auto-capitalize :none
       :on-clear        clear-input
       :return-key-type :done
       :clearable?      (not empty-input?)
       :on-change-text  on-change-text

       :button          (when empty-input?
                          {:on-press paste-on-input
                           :text     (i18n/label :t/paste)})
       :value           @input-value}]
     [quo/button
      {:type            :outline
       :on-press        (fn []
                          (rn/dismiss-keyboard!)
                          (rf/dispatch [:open-modal :scan-address]))
       :container-style {:align-self :flex-end}
       :size            40
       :icon-only?      true}
      :i/scan]]))

(defn view
  []
  (let [input-value         (reagent/atom nil)
        clear-input         (fn []
                              (reset! input-value nil)
                              (rf/dispatch [:wallet/clean-scanned-address]))
        paste-on-input      #(clipboard/get-string
                              (fn [clipboard-text]
                                (reset! input-value clipboard-text)))
        customization-color (rf/sub [:profile/customization-color])]
    (rf/dispatch [:wallet/clean-scanned-address])
    (fn []
      [rn/view
       {:style {:flex 1}}
       [quo/page-nav
        {:type      :no-title
         :icon-name :i/close
         :on-press  (fn []
                      (rf/dispatch [:wallet/clean-scanned-address])
                      (rf/dispatch [:navigate-back]))}]
       [quo/text-combinations
        {:container-style style/header-container
         :title           (i18n/label :t/add-address)}]
       [:f> address-input
        {:input-value    input-value
         :clear-input    clear-input
         :paste-on-input paste-on-input}]
       [quo/button
        {:customization-color customization-color
         :disabled?           (string/blank? @input-value)
         :on-press            #(rf/dispatch [:navigate-to
                                             :confirm-address-to-watch
                                             {:address @input-value}])
         :container-style     style/button-container}
        (i18n/label :t/continue)]])))
