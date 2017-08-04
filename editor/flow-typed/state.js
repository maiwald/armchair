declare type State = {
  dialogue: DialogueState
};

declare type DialogueState = {
  selectedLineId: ?number,
  isInSelectionMode: boolean,
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

declare type DialogueNode = {|
  id: string,
  label: string,
  level: number,
  group: string
|};

declare type DialogueEdge = {|
  from: string,
  to: string
|};
