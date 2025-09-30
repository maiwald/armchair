> [!IMPORTANT]
> Armchair is discontinued. It was fun but no more changes will be made.
> 
> Feel free to look around the code or the [last build](https://maiwald.github.io/armchair).

# Armchair

Armchair will be an Editor for games with branching dialogue.

I have rather intricate plans for what I want to to with dialogue in a little
game I am working on that an off-the-shelf editor was not an option. Let's see where that goes ;)

## Development

You need [node](https://nodejs.org/) installed on your system.

Install all dependencies, then start the two processes:

```bash
npm install
npm run dev
```

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build

To compile clojurescript to javascript:

```
make build
```

## GitHub Pages Deployment

This repository includes a GitHub Actions workflow that automatically builds and deploys to GitHub Pages.

### Setup

1. Go to your repository Settings → Pages
2. Under "Build and deployment", select "GitHub Actions" as the source
3. Push to the `main` branch or manually trigger the workflow

The workflow will:
- Build the ClojureScript, Sass, and Tailwind CSS
- Version the output files using the git commit hash
- Deploy the `build` folder to GitHub Pages

For more details, see [.github/workflows/README.md](.github/workflows/README.md).

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
- https://kenney.nl/assets/roguelike-rpg-pack
