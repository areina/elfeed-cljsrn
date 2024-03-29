# elfeed-cljsrn

A mobile client for [Elfeed](https://github.com/skeeto/elfeed/) built with React
Native and written in ClojureScript.

## Download

Elfeed for Android is available on
[Google Play](https://play.google.com/store/apps/details?id=com.elfeedcljsrn),
[F-Droid](https://f-droid.org/packages/com.elfeedcljsrn/)
or you can download the APKs directly
from [Github releases](https://github.com/areina/elfeed-cljsrn/releases/latest)

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

After that, it's possible to connect to a running REPL. If you are using Emacs
and Cider, you can execute:

- `cider-connect-cljs`

## Testing

At this moment, there are just a few unit tests in projects. Events/handlers are
tested, where most of the logic of the application is happening.

### Running the tests

`npx shadow-cljs watch test`

## Managing dependencies

```sh
clojure -Tantq outdated :upgrade true
npm outdated
cd react-native-app
npm outdated
```

## Local release/installation

- `npm run release`
