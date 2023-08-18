(ns quo2.components.wallet.transaction-progress.view
  (:require [quo2.components.wallet.transaction-progress.style :as style]
            [quo2.components.icon :as icons]
            [quo2.components.tags.context-tag.view :as context-tag]
            [quo2.components.selectors.selectors.view :as selectors]
            [quo2.components.buttons.button.view :as button]
            [quo2.components.wallet.progress-bar.view :as progress-box]
            [quo2.components.markdown.text :as text]
            [quo2.components.tags.status-tags :as status-tag]
            [quo2.foundations.colors :as colors]
            [status-im2.common.resources :as resources]
            [quo2.theme :as quo.theme]
            [reagent.core :as reagent]
            [react-native.core :as rn]))

(defn load-icon
  [icon color]
  [rn/view {:style style/icon}
   [icons/icon icon
    {:color color}]])

(def total-box 85)
(def interval-ms 50)

(def lcounter (reagent/atom 0))
(def interval-id (reagent/atom nil))

(defn stop-interval
  []
  (when @interval-id
    (js/clearInterval @interval-id)
    (reset! interval-id nil)))

(defn clear-counter
  []
  (reset! lcounter 0))

(defn update-counter
  [network-state]
  (let [new-counter-value (-> @lcounter inc)]
    (println new-counter-value "arjun")
    (if (or (and (= network-state :pending) (> new-counter-value 0))
            (and (= network-state :sending) (> new-counter-value 2))
            (and (= network-state :confirmed) (> new-counter-value 4))
            (and (= network-state :finalising) (> new-counter-value 18))
            (and (= network-state :finalized) (> new-counter-value total-box))
            (and (= network-state :error) (> new-counter-value 2)))
      (stop-interval)
      (swap! lcounter (fn [_] new-counter-value)))))

(defn start-interval
  [network-state]
  (reset! interval-id
    (js/setInterval
     (fn []
       (update-counter network-state))
     interval-ms)))

(defn calculate-box-state
  [network-state counter index]
  (cond
    (and (= network-state :sending) (>= counter index) (< index 3))                 :confirmed
    (and (= network-state :confirmed) (>= counter index) (< index 5))               :confirmed
    (and (= network-state :finalising) (>= counter index) (< index 5))              :confirmed
    (and (= network-state :finalising) (>= counter index) (> index 4) (< index 20)) :finalized
    (and (= network-state :finalized) (>= counter index) (< index 5))               :confirmed
    (and (= network-state :finalized) (>= counter index) (> index 4))               :finalized
    (and (= network-state :error) (>= counter index) (< index 2))                   :error
    :else                                                                           :pending))

(defn progress-boxes
  [network-state]
  [rn/view
   {:style style/progress-box-container}
   (let [numbers (range 1 total-box)]
     (doall (for [n numbers]
              [progress-box/view
               {:state               (calculate-box-state network-state @lcounter n)
                :customization-color :blue
                :key                 n}])))])

(defn calculate-box-state-arbitrum
  [network-state network-type]
  (cond
    (and (= network-type :arbitrum) (= network-state :sending)) :confirmed
    (or (= network-state :pending) (= network-state :sending))  :pending
    (= network-state :error)                                    :error
    :else                                                       :confirmed))

(defn calculate-box-width
  [showHalf]
  (cond
    (and showHalf (< @lcounter 30)) (- total-box @lcounter)
    showHalf                        30
    (< @lcounter total-box)         (- total-box @lcounter)
    :else                           0))

(defn progress-boxes-arbitrum
  [network-state network-type]
  [rn/view
   {:style style/progress-box-container}
   [progress-box/view
    {:state               (calculate-box-state-arbitrum network-state network-type)
     :customization-color :blue}]
   [rn/view
    {:style            style/progress-box-arbitrum
     :background-color (colors/theme-colors colors/white colors/neutral-5 colors/neutral-70)
     :border-color     (colors/theme-colors colors/white colors/neutral-10 colors/neutral-80)}
    [rn/view
     (assoc
      (let [box-style (cond
                        (= network-state :finalising) (assoc {:style style/progress-box-arbitrum-abs}
                                                             :right (str (calculate-box-width true) "%")
                                                             :background-color
                                                             (colors/theme-colors
                                                              (colors/custom-color :blue 50)
                                                              (colors/custom-color :blue 60)
                                                              quo.theme))
                        (= network-state :finalized)  (assoc {:style style/progress-box-arbitrum-abs}
                                                             :right (str (calculate-box-width false) "%")
                                                             :background-color
                                                             (colors/theme-colors
                                                              (colors/custom-color :blue 50)
                                                              (colors/custom-color :blue 60)
                                                              quo.theme))
                        :else                         (assoc {:style style/progress-box-arbitrum-abs}
                                                             :background-color
                                                             (colors/theme-colors colors/white
                                                                                  colors/neutral-5
                                                                                  colors/neutral-70)))]
        box-style)
      :align-self "flex-end"
      :border-color
      (colors/theme-colors colors/white colors/neutral-10 colors/neutral-80))]]])

(defn render-text
  [title override-theme &
   {:keys [typography weight size style]
    :or   {typography :main-semibold
           weight     :semi-bold
           size       :paragraph-1
           style      style/title}}]
  [text/text
   {:typography          typography
    :accessibility-label :title-name-text
    :ellipsize-mode      :tail
    :style               style
    :override-theme      override-theme
    :number-of-lines     1
    :weight              weight
    :size                size}
   title])

(defn network-type-text
  [network-type network-state]
  (cond
    (and (= network-state :sending) (= network-type :arbitrum))     "Confirmed on "
    (or (= network-state :sending) (= network-state :pending))      "Pending on "
    (or (= network-state :confirmed) (= network-state :finalising)) "Confirmed on "
    (= network-state :finalized)                                    "Finalized on "
    (= network-state :error)                                        "Failed on "))

(defn steps-text
  [network-type network-state]
  (cond
    (and (= network-type :mainnet)
         (not= network-state :finalized)
         (not= network-state :error))       (str (if (< @lcounter 4)
                                                   @lcounter
                                                   "4")
                                                 "/4")
    (= network-state :finalized)            "Epoch 181,329"
    (and (= network-type :mainnet)
         (= network-state :error))          "0/4"
    (and (not= network-type :mainnet)
         (or (= network-state :finalising)
             (= network-state :confirmed))) "1/1"
    (and (= network-type :arbitrum)
         (= network-state :sending))        "1/1"
    (not= network-type :mainnet)            "0/1"))

(defn get-status-icon
  [network-type network-state]
  (cond
    (and (= network-type :arbitrum)
         (= network-state :sending))   ["positive-state"
                                        (colors/theme-colors colors/white
                                                             colors/success-50
                                                             colors/success-60)]
    (or (= network-state :pending)
        (= network-state :sending))    ["pending-state"
                                        (colors/theme-colors colors/white
                                                             colors/neutral-50
                                                             colors/neutral-60)]
    (or (= network-state :confirmed)
        (= network-state :finalising)) ["positive-state"
                                        (colors/theme-colors colors/white
                                                             colors/success-50
                                                             colors/success-60)]
    (= network-state :finalized)       ["diamond"
                                        (colors/theme-colors colors/white
                                                             colors/success-50
                                                             colors/success-60)]
    (= network-state :error)           ["negative-state"
                                        (colors/theme-colors colors/danger
                                                             colors/danger-50
                                                             colors/danger-60)]))

(defn render-title
  [network-state title override-theme]
  [rn/view
   {:style style/title-item-container}
   [rn/view
    {:style style/inner-container}
    [load-icon "placeholder" (colors/theme-colors colors/white colors/neutral-50 colors/neutral-60)]
    [rn/view
     {:style style/title-container}
     [render-text title override-theme]]
    (when (= network-state :error)
      [button/button
       {:size      24
        :icon-left :i/refresh
        :type      :primary} "Retry"])]])

(defn render-tag
  [context-icon]
  [rn/view
   {:style style/padding-row}
   [context-tag/context-tag {:blur? [false]}
    (resources/get-mock-image context-icon)
    "Doodle #120"]])

(defn get-network-text
  [network-type]
  (cond
    (= network-type :arbitrum) "Arbitrum"
    (= network-type :mainnet)  "Mainnet"
    (= network-type :optimism) "Optimism"))

(defn render-status-row
  [override-theme network-state network-type]
  [rn/view
   {:style style/item-container}
   [rn/view
    {:style (assoc style/progress-container
                   :border-color
                   (colors/theme-colors colors/white colors/neutral-10 colors/neutral-80))}
    (let [[status-icon color] (get-status-icon network-type network-state)]
      [load-icon status-icon color])
    [rn/view
     {:style style/title-container}
     [render-text (str (network-type-text network-type network-state) (get-network-text network-type))
      override-theme
      :typography
      :typography/font-regular :weight :regular :size :paragraph-2]]
    [rn/view
     [render-text (steps-text network-type network-state) override-theme :typography
      :typography/font-regular :weight :regular :size :paragraph-2 :style
      {:color (colors/theme-colors colors/white colors/neutral-50 colors/neutral-60)}]]]])

(defn transaction-progress
  [{:keys [title
           on-press
           accessibility-label
           network-type
           network-state
           start-interval-now
           context-icon
           override-theme]}]
  (let [count (reagent/atom 0)]
    (rn/use-effect
     (fn []
       (when start-interval-now
         (start-interval network-state))
       (clear-counter)
       (fn []
         (stop-interval)))
     [network-state])
    [rn/view
     [rn/touchable-without-feedback
      {:on-press            on-press
       :accessibility-label accessibility-label}
      [rn/view
       {:style style/box-style}
       [render-title network-state title override-theme]
       [render-tag context-icon]
       (when (= network-type :mainnet)
         [render-status-row override-theme network-state network-type])
       (when (= network-type :optimism-arbitrum)
         [render-status-row override-theme network-state :arbitrum])
       (when (= network-type :optimism-arbitrum)
         [progress-boxes-arbitrum network-state :arbitrum])
       (when (= network-type :optimism-arbitrum)
         [render-status-row override-theme network-state :optimism])
       (when (= network-type :optimism-arbitrum)
         [progress-boxes-arbitrum network-state :optimism])
       (when (= network-type :mainnet)
         [progress-boxes network-state])]]]))
