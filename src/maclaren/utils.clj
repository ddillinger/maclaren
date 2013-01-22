(ns maclaren.utils
  (:require [clojure.java.io :refer [file]])
  (:import (org.apache.commons.io FilenameUtils)
           (org.apache.commons.lang3 StringUtils)))

(defn base-name
  [path]
  (when-not (StringUtils/isEmpty path)
    (FilenameUtils/getBaseName path)))

(defn path-exists? [p]
  (.exists (file p)))
