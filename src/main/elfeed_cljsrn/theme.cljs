(ns elfeed-cljsrn.theme
  (:require ["@react-navigation/native" :rename {DarkTheme NavigationDarkTheme DefaultTheme NavigationDefaultTheme}]
            ["react-native" :as rn]
            ["react-native-paper" :refer [adaptNavigationTheme MD3DarkTheme MD3LightTheme]]))

(def adapted-themes ^js (adaptNavigationTheme #js
                                               {:reactNavigationLight NavigationDefaultTheme,
                                                :reactNavigationDark NavigationDarkTheme}))

(def app-themes #js {:light
                     #js
                      {:colors
                       #js
                        {:primary "rgb(0, 106, 96)",
                         :onPrimary "rgb(255, 255, 255)",
                         :primaryContainer "rgb(116, 248, 229)",
                         :onPrimaryContainer "rgb(0, 32, 28)",
                         :secondary "rgb(74, 99, 95)",
                         :onSecondary "rgb(255, 255, 255)",
                         :secondaryContainer "rgb(204, 232, 226)",
                         :onSecondaryContainer "rgb(5, 32, 28)",
                         :tertiary "rgb(69, 97, 121)",
                         :onTertiary "rgb(255, 255, 255)",
                         :tertiaryContainer "rgb(204, 229, 255)",
                         :onTertiaryContainer "rgb(0, 30, 49)",
                         :error "rgb(186, 26, 26)",
                         :onError "rgb(255, 255, 255)",
                         :errorContainer "rgb(255, 218, 214)",
                         :onErrorContainer "rgb(65, 0, 2)",
                         :background "rgb(250, 253, 251)",
                         :onBackground "rgb(25, 28, 27)",
                         :surface "rgb(250, 253, 251)",
                         :onSurface "rgb(25, 28, 27)",
                         :surfaceVariant "rgb(218, 229, 225)",
                         :onSurfaceVariant "rgb(63, 73, 71)",
                         :outline "rgb(111, 121, 119)",
                         :outlineVariant "rgb(190, 201, 198)",
                         :shadow "rgb(0, 0, 0)",
                         :scrim "rgb(0, 0, 0)",
                         :inverseSurface "rgb(45, 49, 48)",
                         :inverseOnSurface "rgb(239, 241, 239)",
                         :inversePrimary "rgb(83, 219, 201)",
                         :elevation
                         #js
                          {:level0 "transparent",
                           :level1 "rgb(238, 246, 243)",
                           :level2 "rgb(230, 241, 239)",
                           :level3 "rgb(223, 237, 234)",
                           :level4 "rgb(220, 235, 232)",
                           :level5 "rgb(215, 232, 229)"},
                         :surfaceDisabled "rgba(25, 28, 27, 0.12)",
                         :onSurfaceDisabled "rgba(25, 28, 27, 0.38)",
                         :backdrop "rgba(41, 50, 48, 0.4)"}},
                     :dark
                     #js
                      {:colors
                       #js
                        {:primary "rgb(83, 219, 201)",
                         :onPrimary "rgb(0, 55, 49)",
                         :primaryContainer "rgb(0, 80, 72)",
                         :onPrimaryContainer "rgb(116, 248, 229)",
                         :secondary "rgb(177, 204, 198)",
                         :onSecondary "rgb(28, 53, 49)",
                         :secondaryContainer "rgb(51, 75, 71)",
                         :onSecondaryContainer "rgb(204, 232, 226)",
                         :tertiary "rgb(173, 202, 230)",
                         :onTertiary "rgb(21, 51, 73)",
                         :tertiaryContainer "rgb(45, 73, 97)",
                         :onTertiaryContainer "rgb(204, 229, 255)",
                         :error "rgb(255, 180, 171)",
                         :onError "rgb(105, 0, 5)",
                         :errorContainer "rgb(147, 0, 10)",
                         :onErrorContainer "rgb(255, 180, 171)",
                         :background "rgb(25, 28, 27)",
                         :onBackground "rgb(224, 227, 225)",
                         :surface "rgb(25, 28, 27)",
                         :onSurface "rgb(224, 227, 225)",
                         :surfaceVariant "rgb(63, 73, 71)",
                         :onSurfaceVariant "rgb(190, 201, 198)",
                         :outline "rgb(137, 147, 144)",
                         :outlineVariant "rgb(63, 73, 71)",
                         :shadow "rgb(0, 0, 0)",
                         :scrim "rgb(0, 0, 0)",
                         :inverseSurface "rgb(224, 227, 225)",
                         :inverseOnSurface "rgb(45, 49, 48)",
                         :inversePrimary "rgb(0, 106, 96)",
                         :elevation
                         #js
                          {:level0 "transparent",
                           :level1 "rgb(28, 38, 36)",
                           :level2 "rgb(30, 43, 41)",
                           :level3 "rgb(31, 49, 46)",
                           :level4 "rgb(32, 51, 48)",
                           :level5 "rgb(33, 55, 51)"},
                         :surfaceDisabled "rgba(224, 227, 225, 0.12)",
                         :onSurfaceDisabled "rgba(224, 227, 225, 0.38)",
                         :backdrop "rgba(41, 50, 48, 0.4)"}}})

(def combined-default-theme
  (.assign js/Object
           MD3LightTheme
           (.-LightTheme adapted-themes)
           #js {:colors (.assign js/Object
                                 (.-colors MD3LightTheme)
                                 (.-colors (.-LightTheme adapted-themes))
                                 (.-colors ^js (.-light app-themes)))}))

(def combined-dark-theme
  (.assign js/Object
           MD3DarkTheme
           (.-DarkTheme adapted-themes)
           #js {:colors (.assign js/Object
                                 (.-colors MD3DarkTheme)
                                 (.-colors (.-DarkTheme adapted-themes))
                                 (.-colors ^js (.-dark app-themes)))}))

(def modes {"dark" combined-dark-theme
            "light" combined-default-theme})

(defn get-app-theme []
  (let [color-scheme (.getColorScheme rn/Appearance)]
    (get modes color-scheme combined-default-theme)))
