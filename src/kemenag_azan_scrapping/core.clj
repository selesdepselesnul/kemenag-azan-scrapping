(ns kemenag-azan-scrapping.core
  (:require [org.httpkit.client :as http]
            [net.cgrand.enlive-html :as enl-html]
            [clojure.data.json :as json]
            [clojure.string :as str]))

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

(defn post-kemenag [url params f-success]
  (let [options { :headers {"Host" "sihat.kemenag.go.id"
                            "Origin" "http://sihat.kemenag.go.id"
                            "Referer" "http://sihat.kemenag.go.id/waktu-sholat"}
                 :form-params params }
        {:keys [status headers body error] :as resp}
        @(http/post url options)]
    (if error
      nil
      (f-success body))))

(defn get-cities-sync [province]
  (post-kemenag "http://sihat.kemenag.go.id/site/get_kota_lintang"
                {"q" province}
                #(html-string->cities %)))

(defn get-azans-sync [year month location & date]
  (post-kemenag "http://sihat.kemenag.go.id/site/get_waktu_sholat"
                {"tahun" year
                 "bulan" month
                 "lokasi" location}
                (fn [x]
                  (let [azans (get (json/read-str x) "data")]
                    (if (nil? date)
                      azans
                      (get azans (first date)))))))

(defn now []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd")
           (new java.util.Date)))

(defn get-azan-today-sync [location]
  (let [date-now (now)
        splitted-date (str/split date-now #"-")]
    (get-azans-sync (first splitted-date)
                    (second splitted-date)
                    location
                    date-now)))
