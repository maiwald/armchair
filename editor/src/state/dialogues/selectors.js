// @flow
import { map, isNull } from "lodash";
import { Map } from "immutable";
import getLevels from "./level_helper";

export function getDialogue(state: any) {
  return state.get("dialogue");
}

function getLine(state, id) {
  if (isNull(id)) return undefined;

  return getDialogue(state)
    .get("lines")
    .find(line => line.get("id") == id);
}

export function getEmptyLine() {
  return Map({ id: undefined, characterId: undefined, text: "" });
}

export function hasSelectedLine(state: any) {
  return getDialogue(state).get("selectedLineId") != null;
}

export function isInSelectionMode(state: any): boolean {
  return getDialogue(state).get("isInSelectionMode");
}

export function getSelectedLineId(state: any): ?number {
  return getDialogue(state).get("selectedLineId");
}

export function getSelectedLine(state: any) {
  return getLine(state, getSelectedLineId(state));
}

export function getDialogueNodes(state: any): DialogueNode[] {
  const dialogue = getDialogue(state);
  const lines = dialogue.get("lines");
  const connections = dialogue.get("connections");
  const levels = getLevels(lines, connections);

  return lines
    .map(l => {
      return {
        id: l.get("id"),
        label: l.get("text"),
        group: l.get("characterId"),
        level: levels.get(l.get("id"))
      };
    })
    .toJS();
}

export function getDialogueEdges(state: any): DialogueEdge[] {
  const dialogue = getDialogue(state);
  const connections = dialogue.get("connections");
  return connections
    .map(c => {
      return {
        id: c.get("id"),
        from: c.get("from"),
        to: c.get("to")
      };
    })
    .toJS();
}
