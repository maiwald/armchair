// @flow
import { find, includes, isUndefined, pick, reject, without } from "lodash";
import {
  SELECT_LINE,
  CLEAR_LINE_SELECTION,
  START_CONNECTION_SELECTION,
  DELETE_LINE,
  UPDATE_LINE,
  CREATE_LINE,
  PRESS_ESCAPE
} from "state/action_types";
import { getNextId } from "state/utils";

const initialState: DialogueState = {
  selectedLineId: undefined,
  nodeConnectionStart: undefined,
  lines: [
    { id: 1, characterId: 1, text: "Hey, who are you?" },
    { id: 2, characterId: 2, text: "I could ask you the same." },
    { id: 3, characterId: 2, text: "My name does not matter." },
    { id: 4, characterId: 1, text: "I am Hugo. And you...?" },
    { id: 5, characterId: 2, text: "I am Hugo as well." },
    { id: 6, characterId: 2, text: "None of your business!" },
    { id: 7, characterId: 1, text: "Fine, be a jerk." },
    { id: 8, characterId: 1, text: "Nice to meet you!" },
    { id: 9, characterId: 1, text: "Ok, bye!" }
  ],
  connections: [
    { id: 1, from: 1, to: 2 },
    { id: 2, from: 2, to: 4 },
    { id: 3, from: 4, to: 5 },
    { id: 4, from: 4, to: 6 },
    { id: 5, from: 6, to: 7 },
    { id: 6, from: 5, to: 8 },
    { id: 7, from: 8, to: 9 },
    { id: 8, from: 1, to: 3 },
    { id: 9, from: 3, to: 7 },
    { id: 10, from: 7, to: 9 }
  ]
};

function isValidNewConnection(
  state: DialogueState,
  { from, to }: { from: number, to: number }
): boolean {
  if (from == to) {
    return false;
  } else {
    const res = find(state.connections, c => c.from == from && c.to == to);
    return typeof res == "undefined";
  }
}

function getLine(state: DialogueState, lineId: number): Line {
  const line = state.lines.find(l => l.id == lineId);
  if (typeof line == "undefined") {
    throw new Error(`Cannot find line with ID ${lineId}`);
  } else {
    return line;
  }
}

function getLineIndex(state: DialogueState, lineId: number): number {
  return state.lines.findIndex(l => l.id == lineId);
}

export function isInSelectionMode(state: DialogueState): boolean {
  return !isUndefined(state.nodeConnectionStart);
}

export default function reducer(
  state: DialogueState = initialState,
  { type, payload }: Action
): DialogueState {
  switch (type) {
    case CREATE_LINE: {
      const { lineData } = payload;
      const lineId = getNextId(state.lines);
      const line = { ...lineData, id: lineId };

      return {
        ...state,
        lines: [...state["lines"], line],
        selectedLineId: lineId
      };
    }

    case UPDATE_LINE: {
      const { lineId, lineData } = payload;
      const line = { ...lineData, id: lineId };

      return {
        ...state,
        lines: [...without(state.lines, getLine(state, lineId)), line],
        selectedLineId: lineId
      };
    }

    case DELETE_LINE: {
      const { lineId } = payload;

      return {
        ...state,
        selectedLineId:
          state.selectedLineId == lineId ? undefined : state.selectedLineId,
        lines: without(state.lines, getLine(state, lineId)),
        connections: reject(state.connections, c => {
          return [c.from, c.to].includes(lineId);
        })
      };
    }

    case SELECT_LINE: {
      const { lineId } = payload;

      if (typeof state.nodeConnectionStart == "number") {
        const from: number = state.nodeConnectionStart;
        const connectionData = { from: from, to: lineId };

        if (!isValidNewConnection(state, connectionData)) {
          return state;
        } else {
          return {
            ...state,
            nodeConnectionStart: undefined,
            connections: [
              ...state.connections,
              { ...connectionData, id: getNextId(state.connections) }
            ]
          };
        }
      } else {
        return { ...state, selectedLineId: lineId };
      }
    }

    case CLEAR_LINE_SELECTION: {
      return { ...state, selectedLineId: undefined };
    }

    case PRESS_ESCAPE: {
      return {
        ...state,
        selectedLineId: undefined,
        nodeConnectionStart: undefined
      };
    }

    case START_CONNECTION_SELECTION: {
      return {
        ...state,
        nodeConnectionStart: payload
      };
    }

    default: {
      return state;
    }
  }
}
