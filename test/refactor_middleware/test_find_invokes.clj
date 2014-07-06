(ns refactor-middleware.test-find-invokes
  (:require [refactor-middleware.test-util :as u]
            [refactor-middleware.refactor :refer :all]
            [clojure.test :refer :all]))

(def ss-util
  "(ns secret-santa.util)

(defn foobar [x]
  (println \"foobar\" x))")

(def ss-util2
  "(ns secret-santa.util2)

(defn foobar [x]
  (println \"foobar2\" x))")

(def test-ns-string-no-ns-decl
 "(defn foobar [x]
    (println \"foobar: \" x)
    x)")

(def test-ns-string-core-fns
"(ns secret-santa.core
  (:require [clojure.set :refer [difference]]
            [clojure.string :refer [trim]]))

(defn- rnd-pair-up [participants]
  (pr participants)
  (let [receivers (shuffle participants)]
    (partition 2 (interleave participants receivers))))

(defn pair-up [participants]
  (println
   \"pa\"
   \"rtic\"
   \"ipants\"
   participants)
  (trim \" fooobar \")
  (println
   \"just trimmed ' fooobar '\")
  (let [pairs (rnd-pair-up participants)]
    (println
     \"pa\"
     \"irs\"
     pairs)
    (if (some (fn [p] (= (first p) (second p))) pairs)
      (pair-up participants)
      pairs)))")

(def test-ns-string-required-fns-prefix
  "(ns secret-santa.core
  (:require [clojure.set :refer [difference]]
            [print.foo :as pf]
            [clojure.pprint]
            [clojure.string :refer [trim]]))

(defn- rnd-pair-up [participants]
  (clojure.pprint/pprint participants)
  (let [receivers (shuffle participants)]
    (partition 2 (interleave participants receivers))))

(defn pair-up [participants]
  (trim \" fooobar \")
  (pf/print-let [pairs (rnd-pair-up participants)]
                (if (some (fn [p] (= (first p) (second p))) pairs)
                  (pair-up participants)
                  pairs)))")

(def test-ns-string-required-fns-use
  "(ns secret-santa.core
  (:require [clojure.set :refer [difference]]
            [clojure.pprint :refer :all]
            [clojure.string :refer [trim]])
  (:use [print.foo]))

(defn- rnd-pair-up [participants]
  (pprint participants)
  (let [receivers (shuffle participants)]
    (partition 2 (interleave participants receivers))))

(defn pair-up [participants]
  (trim \" fooobar \")
  (print-let [pairs (rnd-pair-up participants)]
                (if (some (fn [p] (= (first p) (second p))) pairs)
                  (pair-up participants)
                  pairs)))")

(def test-ns-string-required-fns-alias
  "(ns secret-santa.core
  (:require [clojure.set :refer [difference]]
            [secret-santa.util :as u]
            [secret-santa.util2 :as u2]
            [clojure.string :refer [trim]])
  (:use [print.foo]))

(defn- rnd-pair-up [participants]
  (println \"baz\")
  (u2/foobar \"foobar\")
  (let [receivers (shuffle participants)]
    (partition 2 (interleave participants receivers))))

(defn pair-up [participants]
  (u/foobar \"foobar\")
  (print-let [pairs (rnd-pair-up participants)]
                (if (some (fn [p] (= (first p) (second p))) pairs)
                  (pair-up participants)
                  pairs)))")

(def find-invokes #'refactor-middleware.refactor/find-invokes)

(deftest finds-debug-fns-no-ns
  (let [test-ast-debug-fns-no-ns (u/test-ast test-ns-string-no-ns-decl)
        result (find-invokes test-ast-debug-fns-no-ns "println")]
    (println result)

    (is (= 1 (count result)) (format "1 required debug fn was expected but %d found" (count result)))

    (is (= [2] (map first result)) "line numbers don't match")

    (is (= ["println"] (map last result)) "found fn names don't match")

    (is (= [5] (map #(nth % 2) result)) "column numbers don't match")

    (is (= [2] (map second result)) "end line numbers don't match")))

(deftest finds-required-debug-fns-alias
  (let [test-ast-required-fns-alias (u/test-ast test-ns-string-required-fns-alias)
        result (find-invokes test-ast-required-fns-alias "println,secret-santa.util2/foobar")]
    (println result)
    ;; use are?
    (is (= 2 (count result)) (format "2 required debug fn was expected but %d found" (count result)))

    (is (= [9 10] (map first result))  "line numbers don't match")

    (is (= ["println" "secret-santa.util2/foobar"] (map last result)) "found fn names don't match")

    (is (= [3 3] (map #(nth % 2) result)) "column numbers don't match")

    (is (= [9 10] (map second result))  "end line numbers don't match")
    ))

(deftest finds-required-debug-fns-use
  (let [test-ast-required-fns-use (u/test-ast test-ns-string-required-fns-use)
        result (find-invokes test-ast-required-fns-use "print-let,pprint")]
    (println result)

    (is (= 2 (count result)) (format "2 required debug fn was expected but %d found" (count result)))

    (is (= [8 14] (map first result)) "line numbers don't match")

    (is (= ["pprint" "print-let"] (map last result)) "found fn names don't match")

    (is (= [3 3] (map #(nth % 2) result)) "column numbers don't match")

    (is (= [8 17] (map second result)) "end line numbers don't match")))


(deftest finds-required-debug-fns-prefixed
  (let [test-ast-required-fns-prefix (u/test-ast test-ns-string-required-fns-prefix)
        result (find-invokes test-ast-required-fns-prefix "print.foo/print-let,clojure.pprint/pprint")]
    (println result)

    (is (= 2 (count result)) (format "2 required debug fn was expected but %d found" (count result)))

    (is (= [8 14] (map first result)) "line numbers don't match")

    (is (= ["clojure.pprint/pprint" "print.foo/print-let"] (map last result))  "found fn names don't match")

    (is (= [3 3] (map #(nth % 2) result)) "column numbers don't match")

    (is (= [8 17] (map second result)) "end line numbers don't match")))

(deftest finds-core-debug-fns
  (let [test-ast-core-fns (u/test-ast test-ns-string-core-fns)
        result (find-invokes test-ast-core-fns "println,pr,prn")]
    (println result)

    (is (= 4 (count result)) (format "4 core debug fn was expected but %d found" (count result)))

    (is (= [6 11 17 20] (map first result))  "line numbers don't match")

    (is (= ["pr" "println" "println" "println"] (map last result)) "found fn names don't match")

    (is (= [3 3 3 5] (map #(nth % 2) result)) "column numbers don't match")

    (is (= [6 15 18 23] (map second result)) "end line numbers don't match")))
