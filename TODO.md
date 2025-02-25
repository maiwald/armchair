# BUGS
- flickering inputs in character modal
- deleting initial line in dialogue editor does not delete reference
- consistently highlight selected/inspected items
- visual connection idicator is broken in dialogue editor

# TODO
- improved dialogue editor and state management
- select and paint multiple texture sprites
- Dialogue Structure (Quests?)
- zoom in dialogue editor
- sounds & music

# nice things
- consolidate drag-drop logic for location map and location editor
- reliable drag and drop ghosts
- search
- locate resources
- pinch to zoom
- distinguish between data stored in local storage and in files (e.g. location map scroll position)
- validation error notifications and form states
- state invariants
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
