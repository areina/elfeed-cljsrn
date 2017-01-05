# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Changed
- Update ReactNative version to v0.39.2
- Update project clojure/script dependencies
- Update IP in dev environment to work properly with Figwheel and Genymotion emulator

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

[Unreleased]: https://github.com/areina/elfeed-cljsrn/compare/1.1.1...HEAD
[1.1.1]: https://github.com/areina/elfeed-cljsrn/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/areina/elfeed-cljsrn/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/areina/elfeed-cljsrn/compare/c5668e2...1.0.0
