# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [1.2.0]
### Added
- `prep-lib` function for use with `:deps/prep-lib` that reads the deps edn file to dispatch to the correct function

## [1.0.1]
### Fixed
- `javac` function failed to create the compilation directory on some JDK versions

## [1.0.0]
### Added
- `compile-aliases` function to run `javac` multiple times in sequence with different arguments
- `javac` function to compile a set of directories

[1.2.0]: https://github.com/IGJoshua/americano/compare/v1.0.1...v1.2.0
[1.0.1]: https://github.com/IGJoshua/americano/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/IGJoshua/americano/compare/a786c81c970d8b50b10f002aeb773e3b0165ad78...v1.0.0
