(ns status-im2.contexts.wallet.account.tabs.view
  (:require
    [clojure.string :as string]
    [react-native.core :as rn]
    [status-im2.contexts.wallet.account.tabs.about.view :as about]
    [status-im2.contexts.wallet.account.tabs.dapps.view :as dapps]
    [status-im2.contexts.wallet.common.activity-tab.view :as activity]
    [status-im2.contexts.wallet.common.collectibles-tab.view :as collectibles]
    [status-im2.contexts.wallet.common.empty-tab.view :as empty-tab]
    [status-im2.contexts.wallet.common.token-value.view :as token-value]
    [status-im2.contexts.wallet.common.utils :as utils]
    [utils.i18n :as i18n]
    [utils.re-frame :as rf]))

(defn prepare-token
  [{:keys [symbol marketValuesPerCurrency] :as item} color]
  (let [fiat-value                      (utils/total-per-token item)
        marketValues                    (:usd marketValuesPerCurrency)
        {:keys [price changePct24hour]} marketValues
        fiat-change                     (* fiat-value (/ changePct24hour (+ 100 changePct24hour)))]
    {:token               (keyword (string/lower-case symbol))
     :state               :default
     :status              (if (pos? changePct24hour)
                            :positive
                            (if (neg? changePct24hour) :negative :empty))
     :customization-color color
     :values              {:crypto-value      (.toFixed (* fiat-value price) 2)
                           :fiat-value        (utils/prettify-balance fiat-value)
                           :percentage-change (.toFixed changePct24hour 2)
                           :fiat-change       (utils/prettify-balance fiat-change)}}))

(defn parse-tokens
  [tokens color]
  (vec (map #(prepare-token % color) tokens)))

(defn view
  [{:keys [selected-tab account-address account]}]
  (let [tokens              (rf/sub [:wallet/tokens])
        account-tokens      (get tokens (keyword (string/lower-case account-address)))
        customization-color (or (:color-id account) :blue)
        parsed-tokens       (parse-tokens account-tokens customization-color)]
    (case selected-tab
      :assets       [rn/flat-list
                     {:render-fn               token-value/view
                      :data                    parsed-tokens
                      :content-container-style {:padding-horizontal 8}}]
      :collectibles [collectibles/view]
      :activity     [activity/view]
      :permissions  [empty-tab/view
                     {:title        (i18n/label :t/no-permissions)
                      :description  (i18n/label :t/no-collectibles-description)
                      :placeholder? true}]
      :dapps        [dapps/view]
      [about/view])))
