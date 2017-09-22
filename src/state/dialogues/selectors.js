// @flow
import { isUndefined, isNull } from "lodash";
import getLevels from "./level_helper";

export function getDialogue(state: ApplicationState): DialogueState {
  return state.dialogue;
}

function getLine(state: ApplicationState, lineId: ?number): ?Line {
  if (isNull(lineId)) return undefined;
  return getDialogue(state).lines.find(l => l.id == lineId);
}

export function hasSelectedLine(state: ApplicationState): boolean {
  return typeof getDialogue(state).selectedLineId == "number";
}

export function isInSelectionMode(state: ApplicationState): boolean {
  return !isUndefined(getDialogue(state).nodeConnectionStart);
}

export function getSelectedLineId(state: ApplicationState): ?number {
  return getDialogue(state).selectedLineId;
}

export function getHoveredLineId(state: ApplicationState): ?number {
  return getDialogue(state).hoveredLineId;
}

export function getSelectedLine(state: ApplicationState) {
  return getLine(state, getSelectedLineId(state));
}

export function getDialogueNodes(state: ApplicationState): DialogueNode[] {
  const dialogue = getDialogue(state);
  const lines = dialogue.lines;
  const connections = dialogue.connections;
  const levels = getLevels(lines, connections);

  return lines.map(l => {
    return {
      id: l.id.toString(),
      label: l.text,
      group: l.characterId.toString(),
      level: levels[l.id]
    };
  });
}

export function getDialogueEdges(state: ApplicationState): DialogueEdge[] {
  const dialogue = getDialogue(state);
  const connections = dialogue.connections;
  return connections.map(c => {
    return {
      id: c.id.toString(),
      from: c.from.toString(),
      to: c.to.toString()
    };
  });
}
