# Armchair

Armchair will be an Editor for games with branching dialogue.

I have rather intricate plans for what I want to to with dialogue in a little
game I am working on that an off-the-shelf editor was not an option. Let's see where that goes ;)

Anyway, feel free to play around with what it can do so far: https://my-armchair.com/alpha

## Development Mode

Start the dev server using 

```bash
lein figwheel dev
```

Also start a sass watcher

```bash
sass --watch src/sass:resources/public/compiled/css
```

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build

To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

# Credit

All graphic assets are taken from [opengameart.org](https://opengameart.org)

- https://opengameart.org/content/chadmandoo-pixel-city-bros-characters
- https://opengameart.org/content/dungeon-crawl-32x32-tiles
- https://opengameart.org/content/lpc-adobe-building-set
- https://opengameart.org/content/lpc-tile-atlas2
- https://opengameart.org/content/rpg-tiles-cobble-stone-paths-town-objects
- https://opengameart.org/content/basic-map-32x32-by-silver-iv
- https://opengameart.org/content/2d-lost-garden-zelda-style-tiles-resized-to-32x32-with-additions
- https://opengameart.org/content/castle-tiles-for-rpgs
- https://pipoya.itch.io/pipoya-free-rpg-character-sprites-32x32
