(ns basic.core)

(defn handler [request]
  {:content-type "application/edn"
   :body (pr-str (dissoc request :body))})
