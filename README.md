# Mapproc

A Clojure macro that allows very compact description of operations on maps:

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

Note: This is not production quality code and is probably not even a good idea.
I wrote it as an exercise in macro programming.
