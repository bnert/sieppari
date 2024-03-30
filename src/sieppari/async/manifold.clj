(ns sieppari.async.manifold
  (:require [sieppari.async :as sa]
            [manifold.deferred :as d]))

; chain'-, as is being used here, is chain'-/3
; chain'-/3, at the time of writing, has an arglist of:
;  [d x f]
; where:
;  - `d` is a non-realized manifold deferred value, or nil
;    to signal a deferred should be returned/provided
;  - `x` is either a deferred or a value. If it is a deferred,
;    then the deferred is recurively realized until a non-deferred value
;    is yeilded.
;  - `f` is a function applied to the unwrapped value `x`, before being either realized
;    into `d` or being returned as a sucess or error deferred, depending on the result
;    of `(f x)`.
(extend-protocol sa/AsyncContext
  manifold.deferred.Deferred
  (async? [_] true)
  (continue [d f]
    (d/chain'- nil d (sa/-forward-bindings f)))
  (catch [d f]
    (d/catch' d (sa/-forward-bindings f)))
  (await [d] (deref d))

  manifold.deferred.ErrorDeferred
  (async? [_] true)
  (continue [d f]
    (d/chain'- nil d (sa/-forward-bindings f)))
  (catch [d f]
    (d/catch' d (sa/-forward-bindings f)))
  (await [d] (deref d)))
