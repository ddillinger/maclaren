(ns maclaren.upload
  (:require [carica.core :as c]
            [propS3t.core :as s3]
            [clojure.java.io :as io]))

(def max-size (* 1024 1024 20))

;; from utils
(defn limited-inputstream [^java.io.InputStream ins start length]
  (.skip ins start)
  (let [read-count (atom 0)]
    (proxy [java.io.BufferedInputStream] [ins (* 5 1024 1024)]
      (available []
        (min (.available ins) (- length @read-count)))
      (skip [n]
        (let [len (min n (- length @read-count))]
          (.skip ins len)
          (swap! read-count + len)))
      (markSupported [] false)
      (reset [] (throw (java.io.IOException. "mark not supported")))
      (read
        ([]
           (if (= @read-count length)
             -1
             (let [char (proxy-super read)]
               (swap! read-count inc)
               char)))
        ([barray]
           (.read this barray 0 (count barray)))
        ([barray off len]
           (if (= @read-count length)
             -1
             (let [len (min len (- length @read-count))
                   count (proxy-super read barray off len)]
               (swap! read-count + count)
               count)))))))

(defn sections [start end size]
  (lazy-seq
   (let [se (+ start size)]
     (cond
      (> se end)
      [[start end]]
      (= se end)
      []
      :else
      (cons [start se]
            (sections (inc se) end size))))))

(defn upload-file [index-name f]
  (let [f (io/file f)
        object-name (str index-name "/es-index")
        aws {:aws-key (c/config :aws-key)
             :aws-secret-key (c/config :aws-secret-key)}
        bucket (c/config :aws-bucket)]
    (if (> (.length f) max-size)
      (let [exec (java.util.concurrent.Executors/newCachedThreadPool)
            service (java.util.concurrent.ExecutorCompletionService. exec)
            mp (s3/start-multipart aws bucket object-name)
            file-sections (sections 0 (.length f) max-size)]
        (doseq [[i [start end]] (map-indexed vector file-sections)
                :let [^Callable task
                      (fn []
                        (with-open [is (io/input-stream f)
                                    s (limited-inputstream is start end)]
                          (s3/write-part aws mp (inc i) s :length (- end start))))]]
          (.submit service task))
        (.shutdown exec)
        (let [parts (for [_ file-sections] (.get (.take service)))]
          (doall parts)
          (s3/end-multipart aws mp bucket object-name (sort-by :part parts))))
      (with-open [s (io/input-stream f)]
        (s3/write-stream aws bucket object-name s :length (.length f))))
    f))
