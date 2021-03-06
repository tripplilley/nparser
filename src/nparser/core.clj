(ns nparser.core
  (:require [cli-matic.core :refer :all]
            [nparser.generator :refer [container] :as g]
            [nparser.parser :refer :all]
            [nparser.utils :refer :all]
            [nparser.frr.transforms.v2.core :refer [transformer]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            ; [yaml.core :as yaml]
            [clojure.string :as str]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clojure.pprint :refer [pprint]])
  (:gen-class))

(set! *warn-on-reflection* 1)

(timbre/refer-timbre)
(timbre/merge-config! {:appenders {:println {:enabled? false}}})
(timbre/merge-config!
 {:appenders {:spit (appenders/spit-appender {:fname (str/join [*ns* ".log"])})}})


(defn gen-config 
  "Takes a JSON encoded data structure and generate a configuration"
  [arg]
  (debug "gen-config " arg)
  (let [input (if (contains? arg :stdin) 
                  (-> (:stdin arg) (json/parse-string true))
                  (-> (get-file (:file arg) (json/parse-string true))))
        c (g/generator input)]
    (debug "Generator output " c)    
    (println c)))

; (defn gen-yaml [arg]
;   (let [configuration (get-file (:file arg))
;         grammar (get-file "./parsers/frr/v2/frr-new.ebnf")
;         parser (create-parser grammar)
;         t (transformer (parser configuration))
;         z (yaml/generate-string t :dumper-options {:flow-style :block})]
;       (println (str z))))
        ; (yaml/generate-string t)))

(defn gen-json
"Takes a router configuration and parses it into JSON"
 [arg]
    (debug "gen-json " arg)
    (let [input  (if (contains? arg :stdin) (:stdin arg) (get-file (:file arg)))
          grammar (slurp (:grammar arg))
          parser (create-parser grammar)
          t (transformer (parser input))
          z (json/generate-string t)]
      (println (str z))))
    ; (catch Exception e
    ;     (do
    ;       (info "gen-json "(.getMessage e))
    ;       (System/exit 1))))


(def FILE_INPUT
  {:app         {:command     "nparser"
                 :description "A command-line configuration generator"
                 :version     "0.1.3-alpha"}

   :global-opts []

   :commands    [
                 ; {:command     "to-yaml"
                 ;  :description "Generate a YAML file from a config"
                 ;  :opts        [{:option "file" :as "Config input file" :type :string}]
                 ;  :runs        gen-yaml}
                 {:command     "to-json"
                  :description "Generate JSON from a config"
                  :opts        [{:option "file" :as "Config input file" :type :string :default :present}
                                {:option "grammar" :as "Grammar file" :type :string :default :present}]
                  :runs        gen-json}
                 {:command     "to-config"
                  :description "Generate config from an input file"
                  :opts        [{:option "file" :as "JSON input file" :type :string :default :present}]
                  :runs        gen-config}]})


(def STDIN_INPUT
  {:app         {:command     "nparser"
                 :description "A command-line configuration generator"
                 :version     "0.1.2"}

   :global-opts []

   :commands    [
                 {:command     "to-json"
                  :description "Generate JSON from a config"
                  :opts        [{:option "stdin" :as "Config input file" :type :string :default :present}
                                {:option "grammar" :as "Grammar file" :type :string :default :present}]
                  :runs        gen-json}
                 {:command     "to-config"
                  :description "Generate config from an input file"
                  :opts        [{:option "stdin" :as "JSON input file" :type :string :default :present}]
                  :runs        gen-config}]})

(defn -main [& args]
  (let [bf (java.io.BufferedReader. *in*)]
    (if (.ready bf)
        (let [conf (slurp *in*)
              vargs (into [] args)]
          (run-cmd (conj vargs "--stdin" conf) STDIN_INPUT))
        (run-cmd args FILE_INPUT))))
