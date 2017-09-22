declare type ApplicationState = {|
  dialogue: DialogueState,
  characters: CharacterState,
  ui: UiState
|};

declare type DialogueState = {
  selectedLineId: ?number,
  hoveredLineId: ?number,
  nodeConnectionStart: ?number,
  lines: Line[],
  connections: Connection[]
};

declare type Connection = {|
  id: number,
  from: number,
  to: number
|};

declare type Line = {|
  id: number,
  characterId: number,
  text: string
|};

declare type LineData = {
  characterId: number,
  text: string
};

declare type CharacterState = Character[];

declare type Character = {|
  id: number,
  name: string
|};

declare type UiState = {
  notifications: Notification[]
};

declare type Notification = {|
  id: number,
  text: string
|};

declare type DialogueNode = {|
  id: string,
  label: string,
  level: number,
  group: string
|};

declare type DialogueEdge = {|
  id: string,
  from: string,
  to: string
|};
