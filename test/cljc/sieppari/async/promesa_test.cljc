(ns sieppari.async.promesa-test
  (:require [clojure.test :refer [deftest is #?(:cljs async)]]
            [sieppari.async :as as]
            [promesa.core :as p]
            [promesa.exec :as px]))

(deftest async?-test
  (is (as/async? (p/promise 1))))

#?(:clj
   (deftest core-async-continue-clj-callback-test
     (let [respond (promise)
           p (p/create
               (fn [resolve _]
                 (px/schedule! 10 #(resolve "foo"))))]
       (as/continue p respond)
       (is (= @respond "foo"))))
   :cljs
   (deftest core-async-continue-cljs-callback-test
     (let [p (p/create
               (fn [resolve _]
                 (px/schedule! 10 #(resolve "foo"))))]
       (async done
         (is (as/continue p (fn [response]
                              (is (= "foo" response))
                              (done))))))))

#?(:clj
   (deftest core-async-catch-clj-callback-test
     (let [respond (promise)
           p (p/create
               (fn [_ reject]
                 (px/schedule! 10 #(reject (Exception. "fubar")))))]
       (as/catch p (fn [_] (respond "foo")))
       (is (= @respond "foo"))))
   :cljs
   (deftest core-async-catch-cljs-callback-test
     (let [p (p/create
               (fn [_ reject]
                 (px/schedule! 10 #(reject (js/Error. "fubar")))))]
       (async done
         (is (as/continue (as/catch p (fn [_] "foo"))
                          (fn [response]
                            (is (= "foo" response))
                            (done))))))))

#?(:clj
   (deftest await-test
     (is (= "foo"
            (as/await (p/create
                        (fn [resolve _]
                          (px/schedule! 10 #(resolve "foo")))))))))
