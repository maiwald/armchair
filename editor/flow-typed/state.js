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
  text: string,
  level: ?number
|};
