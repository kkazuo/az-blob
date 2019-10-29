(ns az-blob.core
  (:require [clojure.string :as str]
            [hato.client :as h]
            [java-time :as t]
            [clojure.xml :as xml])
  (:import javax.crypto.Mac
           javax.crypto.KeyGenerator
           javax.crypto.spec.SecretKeySpec
           java.security.NoSuchAlgorithmException
           java.security.InvalidKeyException
           java.util.Base64))

(def b64-decoder (Base64/getDecoder))

(def b64-encoder (Base64/getEncoder))

(defn- ts []
  (t/format (t/formatter :rfc-1123-date-time)
            (t/zoned-date-time (t/zone-offset 0))))

(defn conn-str->map
  "Decode Storage connection string into map"
  [x]
  (let [m (->> (str/split x #";")
               (map #(let [[_ k v] (re-matches #"([^=]+)=(.*)" %)]
                       [(keyword k) v]))
               (reduce conj {}))]
    (update m :AccountKey #(.decode b64-decoder %))))

(defn- update-std-header [mac headers name &{:keys [ignore-when]}]
  (when-let [v (get headers name)]
    (when (or (not ignore-when)
              (not= ignore-when v))
      (.update mac (-> v .getBytes))))
  (.update mac (byte 10)))

(defn- update-canonical-headers [mac headers]
  (doseq [k (->> headers keys (filter #(str/starts-with? % "x-ms-")) sort)]
    (.update mac (-> k .getBytes))
    (.update mac (byte 58))
    (.update mac (-> (get headers k) .getBytes))
    (.update mac (byte 10))))

(defn- update-canonical-resource [mac account container resource]
  (.update mac (byte 47))
  (.update mac (.getBytes account))
  (.update mac (byte 47))
  (when container
    (.update mac (.getBytes container)))
  (when resource
    (.update mac (byte 47))
    (.update mac (.getBytes resource))))

(defn- update-canonical-params [mac query]
  (doseq [k (-> query keys sort)]
    (.update mac (byte 10))
    (.update mac (-> k .getBytes))
    (.update mac (byte 58))
    (.update mac (-> (get query k) .getBytes))))

(defn produce-sig [{:keys [secret scheme suffix
                           method headers query
                           account container resource]}]
  (let [algo    "HmacSHA256"
        mac     (Mac/getInstance algo)
        headers (conj {"x-ms-version" "2019-02-02"
                       "x-ms-date"    (ts)}
                      headers)]
    (.init mac (SecretKeySpec. secret algo))
    (.update mac (-> method name str/upper-case .getBytes))
    (.update mac (byte 10))
    (update-std-header mac headers "content-encoding")
    (update-std-header mac headers "content-language")
    ;; Content-Length (empty string when zero)
    (update-std-header mac headers "content-length" :ignore-when "0")
    (update-std-header mac headers "content-md5")
    (update-std-header mac headers "content-type")
    (update-std-header mac headers "date")
    (update-std-header mac headers "if-modified-since")
    (update-std-header mac headers "if-match")
    (update-std-header mac headers "if-none-match")
    (update-std-header mac headers "if-unmodified-since")
    (update-std-header mac headers "range")
    (update-canonical-headers mac headers)
    (update-canonical-resource mac account container resource)
    (update-canonical-params mac query)
    {:method       method
     :url          (str scheme "://" account ".blob." suffix "/"
                        container
                        (when resource
                          (str "/" resource)))
     :query-params (when-not (empty? query) query)
     :headers      (conj headers
                         {"authorization"
                          (str "SharedKey " account ":"
                               (.encodeToString b64-encoder (.doFinal mac)))})}))

(defn list-containers
  "List Containers
  https://docs.microsoft.com/en-us/rest/api/storageservices/list-containers2"
  [account &{:keys [headers query]
             :or   {headers {}
                    query   {}}}]
  (-> (produce-sig {:secret  (:AccountKey account)
                    :account (:AccountName account)
                    :scheme  (:DefaultEndpointsProtocol account)
                    :suffix  (:EndpointSuffix account)
                    :method  :get
                    :headers headers
                    :query   (conj query {"comp" "list"})})
      (conj {:as :stream})
      h/request
      :body
      xml/parse))

(defn list-blobs
  "List Blobs
  https://docs.microsoft.com/en-us/rest/api/storageservices/list-blobs"
  [account container
   &{:keys [headers query]
     :or   {headers {}
            query   {}}}]
  (-> (produce-sig {:secret    (:AccountKey account)
                    :account   (:AccountName account)
                    :scheme    (:DefaultEndpointsProtocol account)
                    :suffix    (:EndpointSuffix account)
                    :container container
                    :method    :get
                    :headers   headers
                    :query     (conj query {"comp"    "list"
                                            "restype" "container"})})
      (conj {:as :stream})
      h/request
      :body
      xml/parse))

(defn get-blob
  "Get Blob
  https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob"
  [account container resource
   &{:keys [headers query]
     :or   {headers {}
            query   {}}}]
  (-> (produce-sig {:secret    (:AccountKey account)
                    :account   (:AccountName account)
                    :scheme    (:DefaultEndpointsProtocol account)
                    :suffix    (:EndpointSuffix account)
                    :container container
                    :resource  resource
                    :method    :get
                    :headers   headers
                    :query     query})
      h/request
      (dissoc :request)))
