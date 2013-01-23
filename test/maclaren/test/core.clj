(ns maclaren.test.core
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [maclaren.core :refer :all]
            [maclaren.utils :refer [rm-r]])
  (:import (java.util UUID)
           (java.util.zip GZIPInputStream)
           (org.apache.commons.compress.archivers.tar TarArchiveInputStream)))

(use-fixtures :once
  ;; Delete anything left over by previous runs. We do this at the
  ;; beginning, instead of the end, so if something fails it leaves
  ;; behind what it did for you to look at.
  (fn [t]
    (rm-r (str (System/getProperty "java.io.tmpdir") "/maclaren-test/"))
    (t)))

(deftest test-create-tarball
  (let [filesdir (io/file (str (System/getProperty "java.io.tmpdir")
                               "/maclaren-test/"
                               (UUID/randomUUID)))]
    (.mkdirs filesdir)
    (dotimes [n 5]
      (spit (str (.getAbsolutePath filesdir) "/" n) "blibber"))
    (testing "create-archive"
      (let [tar (create-archive (.getAbsolutePath filesdir))]
        (is (.exists tar))
        (let [stuff (tar-gz-seq (TarArchiveInputStream.
                                 (GZIPInputStream.
                                  (io/input-stream tar))))]
          (is (seq stuff))
          (is (= 5 (count stuff))))))))

(deftest test-unpack-tarball
  (let [filesdir (io/file (str (System/getProperty "java.io.tmpdir")
                               "/maclaren-test/"
                               (UUID/randomUUID)))
        unpack-dir (io/file (str (System/getProperty "java.io.tmpdir")
                                 "/maclaren-test-out/"
                                 (UUID/randomUUID)))]
    (.mkdirs filesdir)
    (.mkdirs unpack-dir)
    (dotimes [n 5]
      (spit (str (.getAbsolutePath filesdir) "/" n) "blibber"))
    (let [tar (create-archive (.getAbsolutePath filesdir))]
      (unpack-archive tar unpack-dir)
      ;;   5 files in the archive
      ;; + 1 entry for the unpack dir
      ;; + 1 entry for the subdir
      ;; ---
      ;;   7 files in the file-seq
      (is (= 7 (count (file-seq unpack-dir)))))))
