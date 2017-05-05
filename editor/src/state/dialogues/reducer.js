import { fromJS } from "immutable";
import { isUndefined } from "lodash";

const initialState = fromJS({
  selectedLineId: null,
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
  return state.get("lines").map(l => l.get("id")).max() + 1;
}

function getLineIndex(state, lineId) {
  return state.get("lines").findIndex(line => {
    return line.get("id") == lineId;
  });
}

function createLine(state, data) {
  return state.update("lines", lines =>
    lines.push(data.set("id", getNextLineId(state)))
  );
}

function updateLine(state, lineId, data) {
  return state.mergeIn(
    ["lines", getLineIndex(state, lineId)],
    data.set("id", lineId)
  );
}

export default function reducer(state = initialState, { type, payload }) {
  switch (type) {
    case "SAVE_LINE": {
      const { lineId, lineData } = payload;

      return isUndefined(lineId)
        ? createLine(state, lineData)
        : updateLine(state, lineId, lineData);
    }

    case "DELETE_LINE": {
      const { lineId } = payload;

      return state
        .update("selectedLineId", id => {
          return id == lineId ? null : id;
        })
        .update("lines", lines => lines.delete(getLineIndex(state, lineId)))
        .update("connections", connections =>
          connections.filterNot(c => {
            return c.get("from") == lineId || c.get("to") == lineId;
          })
        );
    }

    case "SHOW_LINE_FORM": {
      return state.set("selectedLineId", payload.lineId);
    }

    case "HIDE_LINE_FORM": {
      return state.set("selectedLineId", null);
    }

    default: {
      return state;
    }
  }
}
