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

export function getSelectedLineId(state) {
  return getDialogue(state).get("selectedLineId");
}

export function getSelectedLine(state) {
  return getLine(state, getSelectedLineId(state));
}

export function getOutboundLines(state, lineId) {
  const dialogue = getDialogue(state);
  const childIds = dialogue
    .get("connections")
    .filter(c => c.get("from") == lineId)
    .map(c => c.get("to"));

  return dialogue.get("lines").filter(l => childIds.includes(l.get("id")));
}
