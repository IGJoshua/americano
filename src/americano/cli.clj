(ns americano.cli
  "CLI entrypoints for compiling your Java code from the Clojure CLI."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.tools.deps.alpha :as deps]
   [clojure.tools.deps.alpha.specs :as deps.spec]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (javax.tools JavaCompiler ToolProvider)))

(s/def ::compile-deps ::deps.spec/deps)
(s/def ::source-paths (s/coll-of string?))
(s/def ::output-path string?)
(s/def ::compiler-options (s/coll-of string? :kind vector?))
(s/def ::resource-paths (s/coll-of string? :kind vector?))
(s/def ::include-root-deps? boolean?)
(s/def ::compiler-alias (s/keys :opt-un [::compile-deps ::output-path
                                         ::compiler-options ::source-paths
                                         ::resource-paths ::include-root-deps?]))

(s/def ::deps-map ::deps.spec/deps-map)

(defn- java-path?
  "Checks if a pathname ends in .java."
  [file-or-pathname]
  (str/ends-with? (cond-> file-or-pathname
                    (instance? java.io.File file-or-pathname)
                    (.getName))
                  ".java"))

(defn javac
  "Compiles java code from the given paths, using its own set of dependencies.

  The `opts` map has the following keys:
  - `:compile-deps`, a dependencies map (like the `:deps` key in a `deps.edn`
    file)
  - `:output-path`, a file path to place the resulting class files into (default;
    \"classes\")
  - `:compiler-options`, a vector of options to pass to the compiler
  - `:source-paths`, a vector of directories to find java files in (default;
    [\"src/java\"])
  - `:resource-paths`, a vector of directories to include on the classpath
  - `:include-root-deps?`, a boolean indicating if the project root dependencies
    should be included for compilation (default; true)"
  [opts]
  (if-let [ed (s/explain-data ::compiler-alias opts)]
    (s/explain-printer ed)
    (let [compiler (ToolProvider/getSystemJavaCompiler)]
      (with-open [file-manager (.getStandardFileManager compiler nil nil nil)]
        (let [{:keys [source-paths resource-paths compile-deps output-path compiler-options include-root-deps?]
               :or {compile-deps {}
                    output-path "classes"
                    source-paths ["src/java"]
                    compiler-options []
                    include-root-deps? true}}
              opts
              deps-map (or (:deps-map opts)
                           (let [edn-maps (deps/find-edn-maps)]
                             (deps/merge-edns [(:root-edn edn-maps)
                                               (:user-edn edn-maps)
                                               (:project-edn edn-maps)])))
              lib-map (deps/resolve-deps
                       deps-map
                       (deps/combine-aliases (assoc-in deps-map
                                                       [:aliases ::compile-alias]
                                                       {(if include-root-deps?
                                                          :override-deps
                                                          :replace-deps)
                                                        compile-deps})
                                             [::compile-alias]))
              classpath (deps/make-classpath lib-map (conj source-paths output-path) {:extra-paths resource-paths})
              compiler-options (conj compiler-options
                                     "-d" output-path
                                     "-cp" classpath)
              compilation-units (.getJavaFileObjectsFromFiles
                                 file-manager
                                 (sequence
                                  (comp (map io/file)
                                        (mapcat file-seq)
                                        (filter #(.isFile %))
                                        (filter java-path?))
                                  source-paths))]
          (.mkdir (io/file output-path))
          (.call (.getTask compiler nil file-manager nil compiler-options nil
                           compilation-units)))))))
(s/fdef javac
  :args (s/cat :opts (s/merge ::compiler-alias
                              (s/keys :opt-un [::deps-map]))))

(s/def ::aliases (s/coll-of ::deps.spec/alias))

(defn compile-aliases
  "Compile java code in multiple stages, taking compiler arguments from aliases.

  The `opts` map has the following keys:
  - `:aliases`, a vector of alias names containing argument maps for [[javac]]"
  [{:keys [aliases] :as opts}]
  (if-let [ed (s/explain-data ::aliases aliases)]
    (s/explain-printer ed)
    (let [deps-map (let [edn-maps (deps/find-edn-maps)]
                     (deps/merge-edns [(:root-edn edn-maps)
                                       (:user-edn edn-maps)
                                       (:project-edn edn-maps)]))]
      (doseq [alias aliases
              :let [javac-opts (assoc (get-in deps-map [:aliases alias])
                                      :deps-map deps-map)]]
        (javac javac-opts)))))
(s/fdef compile-aliases
  :args (s/cat :opts (s/keys :req-un [::aliases])))
