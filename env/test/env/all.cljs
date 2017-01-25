(ns ^:figwheel-no-load env.all)

(enable-console-print!)

(js/require "react-native-mock/mock")
(def ReactNative (js/require "react-native"))

;; Some rn components are not mocked correctly
(aset (.-Header (.-NavigationExperimental ReactNative)) "Title" "")
(aset ReactNative "SwipeableListView" (clj->js {}))

;; XMLHttpRequest is not available in nodejs target
;; Use a npm package.
(aset js/GLOBAL "XMLHttpRequest" (.-XMLHttpRequest (js/require "xmlhttprequest")))
