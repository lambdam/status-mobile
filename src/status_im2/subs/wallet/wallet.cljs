(ns status-im2.subs.wallet.wallet
  (:require
    [clojure.string :as string]
    [re-frame.core :as rf]
    [status-im2.contexts.wallet.common.utils :as utils]
    [utils.number]))

(rf/reg-sub
 :wallet/ui
 :<- [:wallet]
 :-> :ui)

(rf/reg-sub
 :wallet/tokens-loading?
 :<- [:wallet/ui]
 :-> :tokens-loading?)

(rf/reg-sub
 :wallet/accounts
 :<- [:wallet]
 :-> #(->> %
           :accounts
           vals
           (sort-by :position)))

(rf/reg-sub
 :wallet/balances
 :<- [:wallet/accounts]
 (fn [accounts]
   (zipmap (map :address accounts)
           (map #(-> % :tokens utils/calculate-balance) accounts))))

(rf/reg-sub
 :wallet/account-cards-data
 :<- [:wallet/accounts]
 :<- [:wallet/balances]
 :<- [:wallet/tokens-loading?]
 (fn [[accounts balances tokens-loading?]]
   (mapv (fn [{:keys [color address] :as account}]
           (assoc account
                  :customization-color color
                  :type                :empty
                  :on-press            #(rf/dispatch [:wallet/navigate-to-account address])
                  :loading?            tokens-loading?
                  :balance             (utils/prettify-balance (get balances address))))
         accounts)))

(rf/reg-sub
 :wallet/current-viewing-account
 :<- [:wallet]
 :<- [:wallet/balances]
 (fn [[{:keys [current-viewing-account-address] :as wallet} balances]]
   (-> wallet
       (get-in [:accounts current-viewing-account-address])
       (assoc :balance (get balances current-viewing-account-address)))))

(defn- calc-token-value
  [{:keys [symbol market-values-per-currency] :as item}]
  (let [fiat-value                        (utils/total-token-value-in-all-chains item)
        market-values                     (:usd market-values-per-currency)
        {:keys [price change-pct-24hour]} market-values
        fiat-change                       (utils/calculate-fiat-change fiat-value change-pct-24hour)]
    {:token               (keyword (string/lower-case symbol))
     :state               :default
     :status              (cond
                            (pos? change-pct-24hour) :positive
                            (neg? change-pct-24hour) :negative
                            :else                    :empty)
     :customization-color :blue
     :values              {:crypto-value      (.toFixed (* fiat-value price) 2)
                           :fiat-value        (utils/prettify-balance fiat-value)
                           :percentage-change (.toFixed change-pct-24hour 2)
                           :fiat-change       (utils/prettify-balance fiat-change)}}))

(rf/reg-sub
 :wallet/account-token-values
 :<- [:wallet]
 :<- [:wallet/tokens]
 (fn [[{:keys [current-viewing-account-address]} tokens]]
   (mapv calc-token-value (get tokens (keyword (string/lower-case current-viewing-account-address))))))
