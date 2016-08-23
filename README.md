# elfeed-cljsrn

A mobile client for [Elfeed](https://github.com/skeeto/elfeed/) built with React
Native and written in ClojureScript. Elfeed is an excellent RSS reader for Emacs
and I was using it daily but now I'm traveling around and I need a basic mobile
app. Also, this was a perfect excuse to play a bit with RN and CLJS. 

This project uses:
* [React Native](https://facebook.github.io/react-native/)
* [Re-frame](https://github.com/Day8/re-frame)
* [Re-natal](https://github.com/drapanjanas/re-natal/)

I'm using Docker and Docker Compose in my development environment because I
didn't want to install npm, js packages and all the Android libraries in my
laptop. I'm not totally comfortable with this way setup and probably it wouldn't
fit very well for iOS development, will see.

## Screenshots

![](https://raw.github.com/areina/elfeed-cljsrn/master/doc/screenshots/elfeed-cljsrn.png)

## Development

I use Emacs for development and Genymotion as an Android emulator.

- `$ script/boot` # Start RN packager and a clojure REPL
- `$ script/dev-build`
- `M-x cider-connect ; localhost ; 7888` # Connect to clojure REPL in port 7888
- `user> (start-figwheel "android")`
- `$ script/install`
- Open the app in Genymotion

## Todo

- Publish on Google Play
- Elfeed: Authentication (auth basic or something more complex)
- Elfeed: Mark entry as read
- App: Update code to new re-frame v0.8
- App: Split code in scenes and components
- App: Testing
- RN: Change navigation to new navigation experimental
- RN: Use SwipeableListView to mark an entry as read from the list
- iOS version
