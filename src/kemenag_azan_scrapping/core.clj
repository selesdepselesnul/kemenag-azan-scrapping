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

(defn get-select [res]
  (if (= :select (:tag res))
    (:content res)
    (recur (-> res
               (get-in [:content])
               first))))

(defn html-string->cities [html-string]
  (let [first-res (->> (html-string->html-resource html-string)
                       first)]
    (->> (get-select first-res)
         (map (fn [x] {:value (get-in x [:attrs :value])
                      :text (:content x)}))
         (filter #(not= "" (:value %))))))

(defn get-cities-sync [province]
  (let [options { :headers {"Host" "sihat.kemenag.go.id"
                            "Origin" "http://sihat.kemenag.go.id"
                            "Referer" "http://sihat.kemenag.go.id/waktu-sholat"}
                 :form-params {"q" province} }
        {:keys [status headers body error] :as resp}
        @(http/post "http://sihat.kemenag.go.id/site/get_kota_lintang" options)]
    (if error
      nil
      (html-string->cities body))))
