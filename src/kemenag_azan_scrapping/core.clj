(ns kemenag-azan-scrapping.core
  (:require [org.httpkit.client :as http]
            [net.cgrand.enlive-html :as enl-html]))

(def kemenag-index (atom nil))

(defn html-string->html-resource [x]
  (enl-html/html-resource (java.io.StringReader. x)))

(defn select-prov [html-resource]
  (enl-html/select html-resource [:#opt_lokasi_provinsi]))

(defn html-string->provinces [body]
  (->> (html-string->html-resource body)
       ((comp first select-prov))
       (mapcat identity)
       last
       (filter map?)
       (map #(:value (:attrs %)))
       (filter #(not= "" %))))

(let [{:keys [status headers body error] :as resp}
      @(http/get "http://sihat.kemenag.go.id/waktu-sholat")]
  (if error
    (println "Failed, exception: " error)
    (swap! kemenag-index (fn [_] (html-string->provinces body)))))
