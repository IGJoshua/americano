# americano
> For when you want to water down your Clojure projects with some Java

[![cljdoc badge](https://cljdoc.org/badge/org.suskalo/americano)](https://cljdoc.org/d/org.suskalo/americano/CURRENT)
[![Clojars Project](https://img.shields.io/clojars/v/org.suskalo/americano.svg)](https://clojars.org/org.suskalo/americano)

Sometimes when writing Clojure code, you need to add a little Java. Many build
tools have various levels of support for this, e.g. when using gradle,
leiningen, or boot. Even the Clojure CLI has some support through community
libraries like [Badigeon](https://github.com/EwenG/badigeon). Unfortunately, the
existing tools for the Clojure CLI require custom code to be written for your
project, and don't follow the declarative data of project definitions otherwise
written with the tool. This library provides a way to compile and use Java code
from your Clojure projects, without writing any build code.

## Installation
This library is available on Clojars, and can be installed as either a Maven
dependency or a Git one in your `deps.edn` file. Just add one of the following
coords as a dependency where it's needed (read on to see where it's needed).

```clojure
org.suskalo/americano {:mvn/version "0.1.0-SNAPSHOT"}
IGJoshua/americano {:git/url "https://github.com/IGJoshua/americano"
                    :sha "a9da9da8e484162042067f19b05e7927b8de709d"}
```

## Usage
In order to use americano, you simply need to define an alias that you will run.

```clojure
:aliases {:compile-java {:replace-deps {org.suskalo/americano {:mvn/version "0.1.0-SNAPSHOT"}}
                         :exec-fn americano.cli/javac
                         :exec-args {:source-paths ["src/java"]}}}
```

This will define an alias that, when run with `-X`, will compile all the .java
source files in the `src/java` directory, outputting them to the `classes`
directory by default.

Additionally, you can use aliases to "store" the arguments when multiple passes
of compilation need to occur.

```clojure
:aliases {:compile-java {:replace-deps {org.suskalo/americano {:mvn/version "0.1.0-SNAPSHOT"}}
                         :exec-fn americano.cli/compile-aliases
                         :exec-args {:aliases [:java/pass-1 :java/pass-2]}}
          :java/pass-1 {:source-paths ["src/java"]
                        :output-path "classes/main"}
          :java/pass-2 {:compile-deps {org.junit.jupiter/junit-jupiter {:mvn/version "5.7.0"}}
                        :source-paths ["test/java"]
                        :output-path "classes/test"
                        :resource-paths ["classes/main"]}}
```

This will run two different compilation steps, one to compile the main java
source, the other to compile tests. As you can see, additional dependencies can
be specified for compilation.

In cases where the dependencies needed at runtime are substantially larger than
those needed for compilation, you can also set the `:include-root-deps?` key to
`false`, which will prevent your project's root dependencies from being on the
classpath during compilation.

## License

Copyright © 2021 Joshua Suskalo

Distributed under the Eclipse Public License version 1.0.
