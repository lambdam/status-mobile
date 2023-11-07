(ns status-im2.contexts.shell.jump-to.events
  (:require
    [re-frame.core :as re-frame]
    [status-im.data-store.switcher-cards :as switcher-cards-store]
    [status-im.utils.core :as utils]
    [status-im2.config :as config]
    [status-im2.constants :as constants]
    [status-im2.contexts.shell.jump-to.animation :as animation]
    [status-im2.contexts.shell.jump-to.constants :as shell.constants]
    [status-im2.contexts.shell.jump-to.state :as state]
    [status-im2.contexts.shell.jump-to.utils :as shell.utils]
    [status-im2.navigation.state :as navigation.state]
    [utils.re-frame :as rf]))

;;;; Effects

;; Navigation
(re-frame/reg-fx
 :shell/change-tab-fx
 (fn [stack-id]
   (when (some #(= stack-id %) shell.constants/stacks-ids)
     (animation/bottom-tab-on-press stack-id false))))

(re-frame/reg-fx
 :shell/navigate-to-jump-to-fx
 (fn []
   (animation/close-home-stack false)
   (when-not config/shell-navigation-disabled?
     (some-> ^js @state/jump-to-list-ref
             (.scrollToOffset #js {:y 0 :animated false})))))

;; Note - pop-to-root resets currently opened screens to `close-screen-without-animation`.
;; This might take some time. So don't directly merge the effect of `pop-to-root` and
;; `navigate-to` for the floating screen. Because it might close even the currently opened screen.
;; https://github.com/status-im/status-mobile/pull/16438#issuecomment-1623954774
(re-frame/reg-fx
 :shell/pop-to-root-fx
 (fn []
   (shell.utils/reset-floating-screens)))

(re-frame/reg-fx
 :shell/reset-state
 (fn []
   (reset! state/floating-screens-state {})))

;;;; Events

;; Switcher
(rf/defn switcher-cards-loaded
  {:events [:shell/switcher-cards-loaded]}
  [{:keys [db]} loaded-switcher-cards]
  {:db (assoc db
              :shell/switcher-cards
              (utils/index-by :card-id (switcher-cards-store/<-rpc loaded-switcher-cards)))})

(defn calculate-card-data
  [db now view-id id]
  (case view-id
    :chat
    (let [chat (get-in db [:chats id])]
      (condp = (:chat-type chat)
        constants/one-to-one-chat-type
        {:card-id       id
         :switcher-card {:type      shell.constants/one-to-one-chat-card
                         :card-id   id
                         :clock     now
                         :screen-id id}}

        constants/private-group-chat-type
        {:card-id       id
         :switcher-card {:type      shell.constants/private-group-chat-card
                         :card-id   id
                         :clock     now
                         :screen-id id}}

        constants/community-chat-type
        {:card-id       (:community-id chat)
         :switcher-card {:type      shell.constants/community-channel-card
                         :card-id   (:community-id chat)
                         :clock     now
                         :screen-id (:chat-id chat)}}

        nil))

    :community-overview
    {:card-id       id
     :switcher-card {:type      shell.constants/community-card
                     :card-id   id
                     :clock     now
                     :screen-id id}}
    nil))

(rf/defn add-switcher-card
  {:events [:shell/add-switcher-card]}
  [{:keys [db now] :as cofx} view-id id]
  (let [card-data     (calculate-card-data db now view-id id)
        switcher-card (:switcher-card card-data)
        card-type     (:type switcher-card)]
    (when card-data
      (rf/merge
       cofx
       (merge
        {:db (assoc-in
              db
              [:shell/switcher-cards (:card-id card-data)]
              switcher-card)}
        (when config/shell-navigation-disabled?
          {:shell/change-tab-fx (cond
                                  (#{shell.constants/one-to-one-chat-card
                                     shell.constants/private-group-chat-card}
                                   card-type)
                                  :chats-stack

                                  :else :communities-stack)}))
       (switcher-cards-store/upsert-switcher-card-rpc switcher-card)))))

(rf/defn close-switcher-card
  {:events [:shell/close-switcher-card]}
  [{:keys [db] :as cofx} card-id]
  (rf/merge
   cofx
   {:db (update db :shell/switcher-cards dissoc card-id)}
   (switcher-cards-store/delete-switcher-card-rpc card-id)))

;; Navigation
(rf/defn navigate-to-jump-to
  {:events [:shell/navigate-to-jump-to]}
  [{:keys [db]}]
  (let [open-floating-screens (shell.utils/open-floating-screens)]
    (merge
     (if config/shell-navigation-disabled?
       {:pop-to-root-fx :shell-stack}
       {:db
        (cond-> db

          (get open-floating-screens shell.constants/chat-screen)
          (assoc-in [:shell/floating-screens shell.constants/chat-screen :animation]
           shell.constants/close-screen-with-shell-animation)

          (and (get open-floating-screens shell.constants/chat-screen)
               (get open-floating-screens shell.constants/community-screen))
          (assoc-in [:shell/floating-screens shell.constants/community-screen :animation]
           shell.constants/close-screen-without-animation)

          (and (not (get open-floating-screens shell.constants/chat-screen))
               (get open-floating-screens shell.constants/community-screen))
          (assoc-in [:shell/floating-screens shell.constants/community-screen :animation]
           shell.constants/close-screen-with-shell-animation)

          (get open-floating-screens shell.constants/discover-communities-screen)
          (assoc-in [:shell/floating-screens shell.constants/discover-communities-screen :animation]
           shell.constants/close-screen-without-animation))

        :dispatch [:set-view-id :shell]})
     {:shell/navigate-to-jump-to-fx nil})))

(rf/defn change-shell-status-bar-style
  {:events [:change-shell-status-bar-style]}
  [_ style]
  {:merge-options {:id "shell-stack" :options {:statusBar {:style style}}}})

(rf/defn change-shell-nav-bar-color
  {:events [:change-shell-nav-bar-color]}
  [_ color]
  {:merge-options {:id "shell-stack" :options {:navigationBar {:backgroundColor color}}}})

(rf/defn shell-navigate-to
  {:events [:shell/navigate-to]}
  [{:keys [db now]} go-to-view-id screen-params animation hidden-screen?]
  (if (shell.utils/shell-navigation? go-to-view-id)
    (let [current-view-id (:view-id db)
          community-id    (get-in db [:chats screen-params :community-id])]
      {:db         (assoc-in
                    db
                    [:shell/floating-screens go-to-view-id]
                    {:id             screen-params
                     :community-id   community-id
                     :hidden-screen? hidden-screen?
                     :clock          now
                     :animation      (or
                                      animation
                                      (cond
                                        (= current-view-id :shell)
                                        shell.constants/open-screen-with-shell-animation
                                        (= current-view-id :chat)
                                        shell.constants/open-screen-without-animation
                                        (= go-to-view-id shell.constants/discover-communities-screen)
                                        shell.constants/open-screen-with-slide-from-bottom-animation
                                        :else
                                        shell.constants/open-screen-with-slide-from-right-animation))})
       :dispatch-n (cond-> []
                     (not hidden-screen?)
                     (conj [:set-view-id go-to-view-id])
                     (and (= go-to-view-id shell.constants/community-screen)
                          (not hidden-screen?)
                          (:current-chat-id db))
                     (conj [:chat/close]))})
    (merge
     {:db          (assoc db :view-id go-to-view-id)
      :navigate-to go-to-view-id}
     (when (and config/shell-navigation-disabled?
                (#{:chat :community-overview} go-to-view-id))
       {:dispatch-later
        ;; 300 ms delay because, navigation is priority over shell card update
        [{:dispatch [:shell/add-switcher-card go-to-view-id screen-params]
          :ms       300}]}))))

(rf/defn shell-navigate-back
  {:events [:shell/navigate-back]}
  [{:keys [db]} animation]
  (let [current-chat-id (:current-chat-id db)
        current-view-id (:view-id db)
        community-id    (when current-chat-id
                          (get-in db [:chats current-chat-id :community-id]))]
    (if (and (not @navigation.state/curr-modal)
             (shell.utils/shell-navigation? current-view-id)
             (seq (shell.utils/open-floating-screens)))
      (merge
       {:db (assoc-in
             db
             [:shell/floating-screens current-view-id :animation]
             (cond
               animation animation
               (= current-view-id shell.constants/discover-communities-screen)
               shell.constants/close-screen-with-slide-to-bottom-animation
               :else
               shell.constants/close-screen-with-slide-to-right-animation))}
       (when (and current-chat-id community-id)
         {:dispatch [:shell/add-switcher-card shell.constants/community-screen community-id]}))
      {:navigate-back nil})))

(rf/defn floating-screen-opened
  {:events [:shell/floating-screen-opened]}
  [{:keys [db]} screen-id id community-id hidden-screen?]
  (merge
   {:db (assoc-in db [:shell/loaded-screens screen-id] true)
    :dispatch-later
    (cond-> []
      community-id
      ;; When opening community chat, open community screen in background
      (conj {:ms       50
             :dispatch [:shell/navigate-to shell.constants/community-screen
                        community-id shell.constants/open-screen-without-animation true]})
      ;; Only update switcher cards for top screen
      (and id (not hidden-screen?))
      (conj {:ms       (* 2 shell.constants/shell-animation-time)
             :dispatch [:shell/add-switcher-card screen-id id]}))}
   (when (and id (not hidden-screen?))
     {:shell/change-tab-fx (if (or (= screen-id shell.constants/community-screen)
                                   community-id)
                             :communities-stack
                             :chats-stack)})))

(rf/defn floating-screen-closed
  {:events [:shell/floating-screen-closed]}
  [{:keys [db]} screen-id]
  {:db         (cond-> (update db :shell/loaded-screens dissoc screen-id)
                 (= screen-id shell.constants/discover-communities-screen)
                 (update :shell/floating-screen dissoc screen-id))
   :dispatch-n (cond-> [[:set-view-id :shell-stack]]
                 (= screen-id shell.constants/chat-screen)
                 (conj [:chat/close]))})
