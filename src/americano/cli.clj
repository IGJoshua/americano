(ns americano.cli
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
(s/def ::source-dirs (s/coll-of string?))
(s/def ::output-path string?)
(s/def ::compiler-options (s/coll-of string? :kind vector?))
(s/def ::compiler-alias (s/keys :opt-un [::compile-deps ::output-path
                                         ::compiler-options ::source-dirs]))

(s/def ::deps-map ::deps.spec/deps-map)

(defn- java-path?
  [file-or-pathname]
  (str/ends-with? (cond-> file-or-pathname
                    (instance? java.io.File file-or-pathname)
                    (.getName))
                  ".java"))

(defn javac
  [opts]
  (if-let [ed (s/explain-data ::compiler-alias opts)]
    (s/explain-printer ed)
    (let [compiler (ToolProvider/getSystemJavaCompiler)]
      (with-open [file-manager (.getStandardFileManager compiler nil nil nil)]
        (let [{:keys [source-dirs compile-deps output-path compiler-options]
               :or {compile-deps {}
                    output-path "classes"
                    source-dirs []
                    compiler-options []}}
              opts
              compiler-options (conj compiler-options "-d" output-path)
              deps-map (dissoc (or (:deps-map opts)
                                   (let [edn-maps (deps/find-edn-maps)]
                                     (deps/merge-edns [(:root-edn edn-maps)
                                                       (:user-edn edn-maps)
                                                       (:project-edn edn-maps)])))
                               :aliases)
              lib-map (deps/resolve-deps deps-map {:override-deps compile-deps})
              compilation-units (.getJavaFileObjectsFromFiles
                                 file-manager
                                 (sequence
                                  (comp (map io/file)
                                        (mapcat file-seq)
                                        (filter #(.isFile %))
                                        (filter java-path?))
                                  source-dirs))]
          (.call (.getTask compiler nil file-manager nil compiler-options nil
                           compilation-units)))))))
(s/fdef javac
  :args (s/cat :opts (s/merge ::compiler-alias
                              (s/keys :opt-un [::deps-map]))))

(s/def ::aliases (s/coll-of ::deps.spec/alias))

(defn compile-aliases
  [{:keys [aliases]}]
  (let [deps-map (let [edn-maps (deps/find-edn-maps)]
                   (deps/merge-edns [(:root-edn edn-maps)
                                     (:user-edn edn-maps)
                                     (:project-edn edn-maps)]))]
    (doseq [alias aliases
            :let [javac-opts (assoc (get-in deps-map [:aliases alias])
                                    :deps-map deps-map)]]
     (javac javac-opts))))
(s/fdef compile-aliases
  :args (s/cat :opts (s/keys :req-un [::aliases])))
