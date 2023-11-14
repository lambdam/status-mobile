(ns status-im2.contexts.wallet.common.utils
  (:require [clojure.string :as string]
            [quo.foundations.resources :as quo.resources]
            [status-im2.constants :as constants]))

(defn get-first-name
  [full-name]
  (first (string/split full-name #" ")))

(defn get-balance-by-address
  [balances address]
  (->> balances
       (filter #(= (:address %) address))
       first
       :balance))

(defn get-account-by-address
  [accounts address]
  (some #(when (= (:address %) address) %) accounts))

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

(defn network-names
  [token]
  (let [balances-per-chain (:balancesPerChain token)]
    (mapv (fn [chain-id-keyword]
            (let [chain-id-str (name chain-id-keyword)
                  chain-id     (js/parseInt chain-id-str)]
              (case chain-id
                10    {:source (quo.resources/get-network :optimism)}
                42161 {:source (quo.resources/get-network :arbitrum)}
                5     {:source (quo.resources/get-network :ethereum)}
                1     {:source (quo.resources/get-network :ethereum)}
                :unknown))) ; Default case if the chain-id is not recognized
          (keys balances-per-chain))))
