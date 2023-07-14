# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [2.3.0]
### Changed
- Update ReactNative version to v0.71.11
- Update project dependencies
- Change to shadow-cljs as the cljs build tool
- Update look&feel

### Added
- Dark mode support
- New way to navigate between feed entries

## [1.3.0]
### Changed
- Update ReactNative version to v0.41.2

### Added
- Add feeds info in navigation drawer
- Add testing setup

## [1.2.0] - 2017-01-05
### Changed
- Clean unnecessary code
- Simplify layout code
- Disable temporarily "swipeable action (mark as read/unread)" for entries due
  to a bug in RN
- Fix layout issues related to the upgrade of RN v0.39
- Update `open-entry-browser` fn to be pure
- Update ReactNative version to v0.39.2
- Update project clojure/script dependencies
- Update IP in dev environment to work properly with Figwheel and Genymotion emulator

### Added
- Add action to mark entry as unread in entry scene
- Add the possibility to select multiple entries and mark them as read/unread in
  entries scene

## [1.1.1] - 2016-11-13
### Fixed
- Fix url validation. Accept urls starting with http:// or https://

## [1.1.0] - 2016-10-23
### Fixed
- Fix entry detail scene with occasional squashed words
- Validate url in configure scene

### Changed
- Change the view when there are no entries to show
- Add visual feedback pushing the refresh button when the entries list is empty

### Added
- Add search text field
- Splash screen

## [1.0.0] - 2016-09-21
### Added
- Scene for configure the url of your Elfeed running instance
- List of feed entries
- Feed entry detail
- Settings scene for update your Elfeed url

[Unreleased]: https://github.com/areina/elfeed-cljsrn/compare/1.3.0...HEAD
[1.3.0]: https://github.com/areina/elfeed-cljsrn/compare/1.2.0...1.3.0
[1.2.0]: https://github.com/areina/elfeed-cljsrn/compare/1.1.1...1.2.0
[1.1.1]: https://github.com/areina/elfeed-cljsrn/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/areina/elfeed-cljsrn/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/areina/elfeed-cljsrn/compare/c5668e2...1.0.0
