# elfeed-cljsrn

[![CircleCI](https://circleci.com/gh/areina/elfeed-cljsrn.svg?style=svg)](https://circleci.com/gh/areina/elfeed-cljsrn)

A mobile client for [Elfeed](https://github.com/skeeto/elfeed/) built with React
Native and written in ClojureScript.

## Download

Elfeed for Android is available
in [Google Play](https://play.google.com/store/apps/details?id=com.elfeedcljsrn)
or you can download the APKs directly
from [Github releases](https://github.com/areina/elfeed-cljsrn/releases)

## Motivation

Elfeed is an excellent RSS reader for Emacs and I was using it daily but now I'm
traveling around and I need a basic mobile app. Also, this was a perfect excuse
to play a bit with React Native and ClojureScript.

This project uses:
* [React Native](https://facebook.github.io/react-native)
* [Reagent](https://github.com/reagent-project/reagent)
* [re-frame](https://github.com/Day8/re-frame)
* [shadow-cljs](https://github.com/thheller/shadow-cljs)

## Screenshots

![](https://raw.github.com/areina/elfeed-cljsrn/master/doc/images/screenshots/elfeed-cljsrn.png)

## Development

First, you need to start shadow-cljs to compile the project:

- `npm run watch`

Once shadow-cljs is running, we can start metro (react native) and run the app in a device:

- `npm start`

You can connect to a running REPL. Using Emacs and Cider, you can execute: 

- `cider-connect-cljs`



## Testing

At this moment, there are almost no tests in the project. The idea is add unit
tests for event handlers, where almost all the logic of the app is happening.
Later, will see if it's possible to add some kind of tests for views, trying to
introduce the idea of snapshot testing, as React Native is doing
with [Jest](https://facebook.github.io/jest/docs/tutorial-react-native.html).

### Running the tests

`$ lein doo node test once`

## Managing dependencies

```sh
clojure -Tantq outdated :upgrade true
npm outdated
cd react-native-app
npm outdated
```

## Local release/installation

- `npm run release`

- `rm -rf react-native-app/app/`
- `npx shadow-cljs release app`
- `./react-native-app/android/gradlew assembleRelease`
- `adb install react-native-app/android/app/build/outputs/apk/release/app-release.apk`

## Todo

- ~~Publish on Google Play~~
- Elfeed: Authentication (auth basic or something more complex)
- ~~Elfeed: Mark entry as read~~
- ~~App: Update code to new re-frame v0.8~~
- ~~App: Split code in scenes and components~~
- App: Testing
- ~~RN: Change navigation to new navigation experimental~~
- ~~RN: Use SwipeableListView to mark an entry as read from the list~~
- iOS version (I won't work on this because I don't use iOS, but I'm happy
  to help you if you are interested)
- Version for tablets
