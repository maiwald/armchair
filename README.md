# Armchair

Armchair will be an Editor for complex branching dialogue in games.

I have a rather intricate plans for what I want to to with dialogue in a little
game I am working on that an off-the-shelf editor was not an option. :)

## Development Mode

Start your repl 
```
lein repl
```

and then start everything in there. 

```clojure
(start)
```

I don't remember why I don't just use `lein figwheel dev` anymore...

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build

To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
