(ns ws-on-change.core
  (:require [juxt.dirwatch :refer [watch-dir]]
            [aleph.http :as http]
            [manifold.stream :as s])
  (:gen-class))

(def change-stream (s/stream))

(defn handle-file-change [filename]
  (fn [msg]
    (when (= :modify (:action msg))
      (let [file (:file msg)]
        (if (= (.getName file) filename)
          (let [content (slurp file)]
            (s/put! change-stream content)
            (clojure.pprint/pprint content)))))))

(defn setup-file-listener [path filename]
  (println "Listening for changes in " path)
  (watch-dir (handle-file-change filename) (clojure.java.io/file path)))

(defn get-directory-and-filename [path]
  (let [splitted (clojure.string/split path #"/")
        filename (last splitted)
        directory-splitted (butlast splitted)]
    {:filename filename
     :directory (clojure.string/join "/" directory-splitted)}))

(comment
  (get-directory-and-filename "src/ws_on_change/core.clj"))

(defn echo-handler [req]
  (let [socket @(http/websocket-connection req)]
    (s/connect change-stream socket)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [path (first args)]
    (if (not path)
      (println "Provide the file path to watch as the first argument")
      (let [dir-conf (get-directory-and-filename path)]
        (setup-file-listener (:directory dir-conf) (:filename dir-conf))
        (http/start-server echo-handler {:port 9090})
        (future)))))
