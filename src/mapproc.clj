(ns mapproc)

(defn deref? [form]
  "Is form a dereference: '@xxx'?"
  (and (seq? form)
       (= (first form) 'clojure.core/deref)))

(defn mapproc-sub [mapname form]
  "Substitute key references (@:keyword or @[:nested :keyword]) by the code
  that retrieves the value from the map."
  (let [recurse #(mapproc-sub mapname %)]
    (cond
      (deref? form)
        (let [ref (second form)]
          (cond
            (keyword? ref)
              `(~ref ~mapname)
            (symbol? ref)
              `(get ~mapname ~ref)
            (vector? ref)
              `(get-in ~mapname ~ref)
            :else
              (throw (IllegalArgumentException.
                       (str "unsupported ref: @" ref)))))
      (seq? form)
        (map recurse form)
      (vector? form)
        (mapv recurse form)
      (set? form)
        (set (map recurse form))
      (map? form)
        (reduce-kv
          (fn [result k v] (assoc result (recurse k) (recurse v)))
          {} form)
      :else
        form)))

(defn preprocess-step
  "Preprocess one step (target + expression).

  Key references (@:keyword or @[:nested :keyword]) in the expression are
  replaced by values from the map. If the target contains a key reference, the
  map itself is instead replaced with the updated map."
  [mapname [target expr]]
  (let [pexpr (mapproc-sub mapname expr)]
    (if (deref? target)
      (let [ref (second target)]
        (cond
          (or (keyword? ref)
              (symbol? ref))
            [mapname `(assoc ~mapname ~ref ~pexpr)]
          (vector? ref)
            [mapname `(assoc-in ~mapname ~ref ~pexpr)]
          :else
            (throw (IllegalArgumentException.
                     (str "unsupported ref: @" ref)))))
      [target pexpr])))

(defmacro mapproc [expression steps]
  "Apply steps to the result of expression which must be a map.

  Each step is a binding, similar to what 'let' works with. Both sides of the
  binding can refer directly to the content of the map via @:key (or @[:outer
  :inner] for nested maps). Bindings are executed in order and the updated map
  is returned."
  (let
    [mapname (gensym 'map-)
     psteps (apply concat (map #(preprocess-step mapname %)
                               (partition 2 steps)))]
    `(let [~mapname ~expression ~@psteps] ~mapname)))

(def example
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
                 @[:nested :one])]))

(defn -main [] (println example))
