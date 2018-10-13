# Armchair

Armchair will be an Editor for games with branching dialogue.

I have rather intricate plans for what I want to to with dialogue in a little
game I am working on that an off-the-shelf editor was not an option. :)

## Development Mode

Start the dev server using 

```bash
lein figwheel dev
```

Also start a sass watcher

```bash
sass --watch src/sass:resources/public/css/compiled
```

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build

To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

# Credit

https://opengameart.org/content/lpc-tile-atlas2
