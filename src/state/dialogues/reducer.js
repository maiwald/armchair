// @flow
import { isUndefined, without, max } from "lodash";

import {
  SELECT_LINE,
  HOVER_LINE,
  SET_MODAL_SELECTION,
  DELETE_LINE,
  UPDATE_LINE,
  CREATE_LINE,
  PRESS_ESCAPE
} from "state/action_types";

const initialState: DialogueState = {
  selectedLineId: undefined,
  hoveredLineId: undefined,
  isInSelectionMode: false,
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

function getNextLineId(state: DialogueState): number {
  return max(state.lines.map(l => l.id)) + 1;
}

function getNextConnectionId(state: DialogueState): number {
  return max(state.connections.map(c => c.id)) + 1;
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

export default function reducer(
  state: DialogueState = initialState,
  { type, payload }: Action
): DialogueState {
  switch (type) {
    case CREATE_LINE: {
      const { lineData } = payload;
      const lineId = getNextLineId(state);
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
          state.selectedLineId == lineId ? null : state.selectedLineId,
        hoveredLineId:
          state.hoveredLineId == lineId ? null : state.hoveredLineId,
        lines: without(state.lines, getLine(state, lineId)),
        connections: state.connections.filter(c => {
          return ![c.from, c.to].includes(lineId);
        })
      };
    }

    case SELECT_LINE: {
      const { lineId } = payload;

      if (typeof state.selectedLineId == "number" && state.isInSelectionMode) {
        const from: number = state.selectedLineId;

        if (typeof lineId == "undefined") {
          return state;
        }

        if (typeof state.selectedLineId == "number") {
          return {
            ...state,
            isInSelectionMode: false,
            connections: [
              ...state.connections,
              {
                id: getNextConnectionId(state),
                from: from,
                to: lineId
              }
            ]
          };
        }
      } else {
        return { ...state, selectedLineId: lineId };
      }
    }

    case HOVER_LINE: {
      const { lineId } = payload;

      if (state.isInSelectionMode) {
        return state;
      } else {
        return { ...state, hoveredLineId: lineId };
      }
    }

    case SET_MODAL_SELECTION: {
      return { ...state, isInSelectionMode: payload };
    }

    case PRESS_ESCAPE: {
      return state.isInSelectionMode
        ? { ...state, isInSelectionMode: false }
        : state;
    }

    default: {
      return state;
    }
  }
}
