(ns kemenag-azan-scrapping.core
  (:require [org.httpkit.client :as http]
            [net.cgrand.enlive-html :as enl-html]))

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

(defn get-provinces-sync []
  (let [{:keys [status headers body error] :as resp}
        @(http/get "http://sihat.kemenag.go.id/waktu-sholat")]
    (if error
      nil
      (html-string->provinces body))))

(def cities (atom nil))
(def cities-raw (atom nil))

(defn get-select [res]
  (if (= :select (:tag res))
    (:content res)
    (recur (-> res
               (get-in [:content])
               first))))

(defn get-cities [html-string]
  (let [first-res (->> (html-string->html-resource html-string)
                       first)]
    (->> (get-select first-res)
         (map (fn [x] {:value (get-in x [:attrs :value])
                      :text (:content x)}))
         (filter #(not= "" (:value %))))))

(let [options { :headers {"Host" "sihat.kemenag.go.id"
                          "Origin" "http://sihat.kemenag.go.id"
                          "Referer" "http://sihat.kemenag.go.id/waktu-sholat"}
               :form-params {"q" "ACEH"} }
      {:keys [status headers body error] :as resp}
      @(http/post "http://sihat.kemenag.go.id/site/get_kota_lintang" options)]
  (if error
    (println "Failed, exception: " error)
    (swap! cities-raw (fn [_] body))))

(get-cities @cities-raw)
