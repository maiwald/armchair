import { Map, fromJS } from "immutable";
import { isUndefined } from "lodash";

import {
  SELECT_LINE,
  SET_MODAL_SELECTION,
  DELETE_LINE,
  UPDATE_LINE,
  CREATE_LINE,
  PRESS_ESCAPE
} from "state/action_types";

const initialState = fromJS({
  selectedLineId: undefined,
  isModalNodeSelection: false,
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
});

function getNextLineId(state) {
  return state.get("lines").map(c => c.get("id")).max() + 1;
}
function getNextConnectionId(state) {
  return state.get("connections").map(l => l.get("id")).max() + 1;
}

function getLineIndex(state, lineId) {
  return state.get("lines").findIndex(line => {
    return line.get("id") == lineId;
  });
}

export default function reducer(state = initialState, { type, payload }) {
  switch (type) {
    case CREATE_LINE: {
      const { lineData } = payload;
      const lineId = getNextLineId(state);
      const line = fromJS(lineData).set("id", lineId);

      return state
        .update("lines", lines => lines.push(line))
        .set("selectedLineId", lineId);
    }

    case UPDATE_LINE: {
      const { lineId, lineData } = payload;
      const line = fromJS(lineData).set("id", lineId);

      return state
        .setIn(["lines", getLineIndex(state, lineId)], line)
        .set("selectedLineId", lineId);
    }

    case DELETE_LINE: {
      const { lineId } = payload;

      return state
        .update("selectedLineId", id => {
          return id == lineId ? null : id;
        })
        .update("lines", lines => lines.delete(getLineIndex(state, lineId)))
        .update("connections", connections => {
          return connections.filterNot(c => {
            return c.get("from") == lineId || c.get("to") == lineId;
          });
        });
    }

    case SELECT_LINE: {
      if (state.get("isModalNodeSelection")) {
        return state
          .set("isModalNodeSelection", false)
          .update("connections", connections => {
            return connections.push(
              Map({
                id: getNextConnectionId(state),
                from: state.get("selectedLineId"),
                to: payload.lineId
              })
            );
          });
      } else {
        return state.set("selectedLineId", payload.lineId);
      }
    }

    case SET_MODAL_SELECTION: {
      return state.set("isModalNodeSelection", payload);
    }

    case PRESS_ESCAPE: {
      return state.get("isModalNodeSelection")
        ? state.set("isModalNodeSelection", false)
        : state;
    }

    default: {
      return state;
    }
  }
}
