(ns maclaren.core
  (:require [clojure.java.io :refer [copy file input-stream output-stream]]
            [clojure.java.shell :as shell]
            [maclaren.utils :refer :all]
            [maclaren.upload :as upload]
            [maclaren.download :as download]
            )
  (:import (java.util UUID)
           (java.util.zip GZIPInputStream GZIPOutputStream)
           (org.apache.commons.compress.archivers.tar TarArchiveInputStream
                                                      TarArchiveOutputStream
                                                      TarArchiveEntry)))

(defn tar-gz-seq
  "A seq of TarArchiveEntry instances on a TarArchiveInputStream."
  [tis]
  (when-let [item (.getNextTarEntry tis)]
    (cons item (lazy-seq (tar-gz-seq tis)))))

(defn create-tar-archive
  "Takes a folder. Makes a tar out of it. Kill me now."
  [tar-path folder-path]
  (let [out (TarArchiveOutputStream.
             (GZIPOutputStream.
              (output-stream tar-path)))
        folder-base (base-name folder-path)]
    (doseq [inf (file-seq (file folder-path))
            :when (.isFile inf)]
      (let [entry-path (.replaceFirst (.getPath inf)
                                      (str folder-path "/")
                                      (str (base-name folder-path) "/"))
            entry (TarArchiveEntry. entry-path)]
        (.setSize entry (.length inf))
        (.putArchiveEntry out entry)
        (copy (input-stream inf) out)
        (.closeArchiveEntry out)))
    (.finish out)
    (.close out)
    (file tar-path)))

(defn unpack-archive
  "Given a .tar.gz unpack it to out-path."
  [tar-file out-path]
  (let [tar-file (file tar-file)
        out-path (file out-path)]
    (.mkdirs out-path)
    (when-not (.exists tar-file)
      (throw
       (Exception. (format "Uh, '%s' isn't actually there?" tar-file))))
    (when-not (.exists out-path)
      (throw
       (Exception.
        (format "That output path '%s' doesn't seem to be there." out-path))))
    (let [tis (TarArchiveInputStream.
               (GZIPInputStream.
                (input-stream tar-file)))]
      (doseq [entry (tar-gz-seq tis)]
        (let [out-file (file (str out-path "/" (.getName entry)))]
          (.mkdirs (.getParentFile out-file))
          (with-open [outf (output-stream out-file)]
            (let [bytes (byte-array 32768)]
              (loop [nread (.read tis bytes 0 32768)]
                (when (> nread -1)
                  (.write outf bytes 0 nread)
                  (recur (.read tis bytes 0 32768)))))))))))

(defn create-archive
  "Given a folder, makes an archive file for it. Returns
  the File object for the archive."
  [folder-path & [tmpdir]]
  (let [archive-name (str (UUID/randomUUID) ".tar.gz")
        out-path (str (if (and tmpdir (path-exists? tmpdir))
                        tmpdir
                        (System/getProperty "java.io.tmpdir"))
                      "/" archive-name)]
    (create-tar-archive out-path folder-path)))

(defn freeze
  [index-name folder-path & [tmpdir]]
  (let [archive-name (str index-name ".tar.gz")
        out-path (str (if (and tmpdir (path-exists? tmpdir))
                        tmpdir
                        (System/getProperty "java.io.tmpdir"))
                      "/" archive-name)
        archive (create-tar-archive out-path folder-path)]
    (upload/upload-file index-name archive)
    (.delete (file archive))))

(defn thaw
  "Given an s3 url, expands to expand-path."
  [index-name expand-path]
  (doseq [x (reverse (file-seq (file expand-path)))]
    (.delete x))
  (let [local-file (download/download-file index-name)
        result (unpack-archive local-file (.getParent (file expand-path)))]
    (.delete local-file)
    result))
