(ns status-im2.contexts.wallet.common.utils
  (:require [clojure.string :as string]
            [status-im2.constants :as constants]
            [utils.money :as money]
            [utils.number]))

(defn get-first-name
  [full-name]
  (first (string/split full-name #" ")))

(defn prettify-balance
  [balance]
  (str "$" (.toFixed (if (number? balance) balance 0) 2)))

(defn get-derivation-path
  [number-of-accounts]
  (str constants/path-wallet-root "/" number-of-accounts))
(defn format-derivation-path
  [path]
  (string/replace path "/" " / "))

(defn get-formatted-derivation-path
  [number-of-accounts]
  (let [path (get-derivation-path number-of-accounts)]
    (format-derivation-path path)))

(defn- total-raw-balance-in-all-chains [balances-per-chain]
  (->> balances-per-chain
       (map (comp :raw-balance val))
       (reduce money/add)))

(defn- total-token-fiat-value
  "Returns the total token fiat value taking into account all token's chains."
  [{:keys [balances-per-chain decimals market-values-per-currency]}]
  (let [usd-price                 (-> market-values-per-currency :usd :price)
        total-units-in-all-chains (-> balances-per-chain
                                      (total-raw-balance-in-all-chains)
                                      (money/token->unit decimals))]
    (money/crypto->fiat total-units-in-all-chains usd-price)))

(defn calculate-balance-for-account
  [{:keys [tokens] :as _account}]
  (->> tokens
       (map total-token-fiat-value)
       (reduce money/add)))
