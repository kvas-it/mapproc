# Mapproc

A Clojure macro that allows compact description of operations on maps:

```clojure
(mapproc {:foo 1, :bar 2}             ; Start with this map
         [t (+ @:foo @:bar)           ; :foo + :bar -> temporary variable t
          @:baz (let [s @:bar]        ; More elaborate code also possible...
                  (+ t s @:foo))      ; t + :bar + :foo -> :baz
          qux 'long-key               ; create alias for a long key
          @qux (+ @:baz 42)           ; :baz + 42 -> long-key 
          @[:nested :one] 1           ; 1 -> :nested :one (nested map)
          @[:nested :two] (- @qux 5)  ; long-key - 5 -> :nested :two
          @:out                       ; :nested :two - :nested :one -> :answer
            (- @[:nested :two]
               @[:nested :one])])

; => {:foo 1, :bar 2, :baz 6, long-key 48, :nested {:one 1, :two 43}, :out 42}
```

For reference, this will expand into something like this:

```clojure
(let [map-186 {:foo 1, :bar 2}
      t (+ (:foo map-186) (:bar map-186))
      map-186 (assoc map-186 :baz
                     (let [s (:bar map-186)]
                       (+ t s (:foo map-186))))
      qux (quote long-key)
      map-186 (assoc map-186 qux
                     (+ (:baz map-186) 42))
      map-186 (assoc-in map-186 [:nested :one] 1)
      map-186 (assoc-in map-186 [:nested :two]
                        (- (get map-186 qux) 5))
      map-186 (assoc map-186 :out
                     (- (get-in map-186 [:nested :two])
                        (get-in map-186 [:nested :one])))]
  map-186)
```

Note: This is not production quality code and is probably not even a good idea.
I wrote it as an exercise in macro programming.
