(ns status-im2.subs.wallet.wallet
  (:require [re-frame.core :as rf]
            [status-im2.contexts.wallet.common.utils :as utils]
            [utils.number]))

<<<<<<< HEAD
(rf/reg-sub
 :wallet/ui
 :<- [:wallet]
 :-> :ui)

(rf/reg-sub
 :wallet/tokens-loading?
 :<- [:wallet/ui]
 :-> :tokens-loading?)

(rf/reg-sub
=======
(defn- calculate-balance
  [address tokens]
  (let [token  (get tokens (keyword address))
        result (reduce
                (fn [acc item]
                  (let [total-values (* (utils/sum-token-chains item)
                                        (get-in item [:marketValuesPerCurrency :USD :price]))]
                    (+ acc total-values)))
                0
                token)]
    result))

(re-frame/reg-sub
>>>>>>> c8bb0a581 (updates)
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
<<<<<<< HEAD
 (fn [[{:keys [current-viewing-account-address] :as wallet} balances]]
   (-> wallet
       (get-in [:accounts current-viewing-account-address])
<<<<<<< HEAD
       (assoc :balance (get balances current-viewing-account-address)))))
=======
       (assoc :balance (utils/get-balance-by-address balances current-viewing-account-address)))))
=======
 (fn [[accounts balances] [_ account-address]]
   (assoc
    (utils/get-account-by-address accounts account-address)
    :balance
    (utils/get-balance-by-address balances account-address))))

(defn- prepare-token
  [{:keys [symbol marketValuesPerCurrency] :as item}]
  (let [fiat-value                      (utils/sum-token-chains item)
        market-values                   (:usd marketValuesPerCurrency)
        {:keys [price changePct24hour]} market-values
        fiat-change                     (* fiat-value (/ changePct24hour (+ 100 changePct24hour)))]
    {:token               (keyword (string/lower-case symbol))
     :state               :default
     :status              (cond
                            (pos? changePct24hour) :positive
                            (neg? changePct24hour) :negative
                            :else                  :empty)
     :customization-color :blue
     :values              {:crypto-value      (.toFixed (* fiat-value price) 2)
                           :fiat-value        (utils/prettify-balance fiat-value)
                           :percentage-change (.toFixed changePct24hour 2)
                           :fiat-change       (utils/prettify-balance fiat-change)}}))

(re-frame/reg-sub
 :wallet/parsed-tokens
 :<- [:wallet/tokens]
 (fn [tokens [_ account-address]]
   (mapv prepare-token (get tokens (keyword (string/lower-case account-address))))))
>>>>>>> 19c75e91d (review)
>>>>>>> 25ec47428 (review)
