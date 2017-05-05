import { isNull } from "lodash";
import { Map } from "immutable";

export function getDialogue(state) {
  return state.get("dialogue");
}

function getLine(state, id) {
  if (isNull(id)) return undefined;

  return getDialogue(state).get("lines").find(line => line.get("id") == id);
}

export function getEmptyLine() {
  return new Map({ id: undefined, characterId: undefined, text: "" });
}

export function hasSelectedLine(state) {
  return getDialogue(state).get("selectedLineId") != null;
}

export function getSelectedLine(state) {
  const selectedLineId = getDialogue(state).get("selectedLineId");
  return getLine(state, selectedLineId);
}
