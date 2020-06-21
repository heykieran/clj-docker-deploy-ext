(ns package
  (:require
   [clojure.core :as core]
   [clojure.tools.deps.alpha.reader :as deps-reader]
   [clojure.tools.deps.alpha :as cdeps]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [badigeon.bundle :refer [bundle make-out-path]]
   [badigeon.compile :as compile]
   [badigeon.classpath :as classpath]
   [badigeon.jar :as jar]
   [badigeon.zip :as zip]
   [badigeon.utils :refer [make-path]]))

(defn- translate-path
  [target-dir path]
  (let
   [file (io/as-file path)]
    (str (if (.isAbsolute file)
           (.getCanonicalPath file)
           (make-path target-dir (.getPath file))))))

(defn- translate-paths
  [target-dir paths]
  (mapv
   #(translate-path target-dir %)
   paths))

(defn- update-deps-paths
  [target-dir base-deps aliases]
  (let
   [translate-paths-fn
    (partial translate-paths target-dir)]
    (deps-reader/merge-deps
     [base-deps
      {:aliases
       (apply
        merge
        (map
         (fn [alias]
           {alias
            (->
             (cdeps/combine-aliases
              base-deps
              aliases)
             (update
              :paths
              translate-paths-fn)
             (update
              :extra-paths
              translate-paths-fn))})
         aliases))}])))

(defn-
  translate-path-to-absolute
  [target-dir deps-map aliases]
  (let
   [path-sep (System/getProperty "path.separator")]
    (str/join
     path-sep
     (mapv
      (fn [fname]
        (let
         [file (io/as-file fname)]
          (if (.isAbsolute file)
            (.getCanonicalPath file)
            (make-path target-dir (.getPath file)))))
      (str/split
       (classpath/make-classpath
        {:deps-map deps-map
         :aliases aliases})
       (re-pattern path-sep))))))

(defn
  -main [target-dir]
  (let
   [out-path (make-out-path 'app nil)
    deps-map (deps-reader/slurp-deps (str target-dir "/deps.edn"))
    aliases [:main]
    translated-deps-map (update-deps-paths
                         target-dir
                         deps-map
                         aliases)
    classes-path (str (make-path out-path "classes"))
    manifest-path (str (make-path classes-path "META-INF/MANIFEST.MF"))]
    
    (bundle
     out-path
     {:deps-map translated-deps-map
      :aliases aliases
      :libs-path "lib"})

    (compile/compile
     'main.core
     {:compile-path
      classes-path
      :classpath
      (translate-path-to-absolute
       target-dir
       deps-map
       aliases)})
    
    (io/make-parents manifest-path)
    
    (spit manifest-path
          (jar/make-manifest
           'main.core
           {:Class-Path
            (str
             ". "
             (str/join
              " "
              (mapv
               #(str "lib/" (.getName %))
               (.listFiles (io/file "target/app/lib")))))}))
    
    (zip/zip
     classes-path
     (str (make-path out-path "app-runner") ".jar"))))

