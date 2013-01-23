(ns maclaren.download
  (:require [carica.core :as c]
            [propS3t.core :as s3]
            [clojure.java.io :as io]))

;; (defn download-file [url]
;;   (let [u (java.net.URI. url)
;;         object-name (java.net.URLDecoder/decode
;;                      (subs (.getPath u) 1 (count (.getPath u))))
;;         aws {:aws-key (c/config :aws-key)
;;              :aws-secret-key (c/config :aws-secret-key)}
;;         bucket (.getHost u)
;;         f (java.io.File/createTempFile "index" "tgz")]
;;     (with-open [o (s3/read-stream aws bucket object-name)]
;;       (io/copy o f))
;;     f))


(defn download-file [f]
  (let [f (io/file f)
        object-name (str (.getName f) "/es-index")
        aws {:aws-key (c/config :aws-key)
             :aws-secret-key (c/config :aws-secret-key)}
        bucket (c/config :aws-bucket)
        f (java.io.File/createTempFile "index" "tgz")]
    (with-open [o (s3/read-stream aws bucket object-name)]
      (io/copy o f))
    f))
