(ns sieppari.async
  #?(:clj (:refer-clojure :exclude [await]))
  (:require [sieppari.util :refer [exception?]])
  #?(:clj (:import java.util.concurrent.CompletionStage
                   java.util.concurrent.CompletionException
                   java.util.function.Function)))

#?(:clj
  (defn -forward-bindings
    ([f]
     (fn [ctx]
       (with-bindings (or (:bindings ctx) {})
         ((bound-fn* f) ctx))))
    ([f ctx]
     (with-bindings (or (:bindings ctx) {})
       ((bound-fn* f) ctx)))))


(defprotocol AsyncContext
  (async? [t])
  (continue [t f])
  (catch [c f])
  #?(:clj (await [t])))

#?(:clj
   (deftype FunctionWrapper [f]
     Function
     (apply [_ v]
       (f v))))

#?(:clj
   (extend-protocol AsyncContext
     Object
     (async? [_] false)
     ; Given the implementation of enter/leave,
     ; `continue` won't be called, and therefore,
     ; the function call does not need to be bound
     ; here.
     (continue [t f] (f t))
     (await [t] t)))

#?(:cljs
   (extend-protocol AsyncContext
     default
     (async? [_] false)
     ; Given the implementation of enter/leave,
     ; `continue` won't be called, and therefore,
     ; the function call does not need to be bound
     ; here.
     (continue [t f] (f t))))

#?(:clj
   (extend-protocol AsyncContext
     clojure.lang.IDeref
     (async? [_] true)
     (continue [c f]
       (future ((-forward-bindings f) @c)))
     (catch [c f]
       (future
         (let [c @c]
           (if (exception? c) ((-forward-bindings f) c) c))))
     (await [c] @c)))

#?(:clj
   (extend-protocol AsyncContext
     CompletionStage
     (async? [_] true)
     (continue [this f]
       ; f may a callable value (i.e. a promise), which doesn't look to
       ; play nicely with with-binding/bound-fn*.
       ;
       ; Therefore, only forwarding bindings when value is
       ; a function and not some other type.
       (.thenApply ^CompletionStage this
                   ^Function (->FunctionWrapper
                               (cond-> f
                                 (fn? f)
                                   (-forward-bindings)))))
     (catch [this f]
       (letfn [(handler [e]
                  (if (instance? CompletionException e)
                   (f (.getCause ^Exception e))
                   (f e)))]
         (.exceptionally ^CompletionStage this
                         ^Function (->FunctionWrapper handler))))

     (await [this]
       (deref this))))

#?(:cljs
   (extend-protocol AsyncContext
     js/Promise
     (async? [_] true)
     (continue [t f] (.then t f))
     (catch [t f] (.catch t f))))
