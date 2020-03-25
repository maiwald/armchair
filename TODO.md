# BUGS
- flickering inputs in character modal
- drag and drop preview images are broken
- deleting initial line in dialogue editor does not delete reference

# TODO
- restructure navigation
  - add ability to jump to dialogue or character from inspector
  - let connections connect to actual entrances and exits
- select and paint multiple texture sprites
- zoom in dialogue editor
- sounds & music

# nice things
- separate moving viewport from moving node positions
- validation error notifications and form states
- state invariants
- remove slds dependency
- save file for game state
- desktop version for easier file access
- release to itch.io as free game ;)

## dialogue editor
- allow creating switches in dialogue editor
- quick-add (with autofocus) for next dialogue node
- prevent connecting multiple trigger and player nodes

## location editor
- allow connections to be bi-directional again
- show incoming connections in location editor
- brush sizes & fill tool for location editor
- custom layers and layer order
- add ability to dim layers to focus on only one
- allow whole layers to be treated as not-walkable
- copy and paste for background texture sets

## game
- show debug state variables in game
- movement behaviour when holding and releasing arrow key
- prevent queueing multiple interactions
- cache static level parts in one image once insted of building it every frame
- show player silhouette when behind foreground texture
- redesign dialogue screen to use pixel font
- typing animation for dialogue screens
