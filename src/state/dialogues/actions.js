// @flow
import { trim, toInteger, isUndefined } from "lodash";
import { isInSelectionMode } from "state/dialogues/selectors";

import {
  SELECT_LINE,
  CLEAR_LINE_SELECTION,
  HOVER_LINE,
  START_CONNECTION_SELECTION,
  DELETE_LINE,
  UPDATE_LINE,
  CREATE_LINE
} from "state/action_types";

export function startConnectionSelection(lineId: number): Action {
  return {
    type: START_CONNECTION_SELECTION,
    payload: lineId
  };
}

export function selectLine(lineId: ?number): Action {
  return {
    type: SELECT_LINE,
    payload: { lineId }
  };
}

export function clearLineSelection(): Action {
  return {
    type: CLEAR_LINE_SELECTION,
    payload: null
  };
}

export function hoverLine(lineId: ?number): Action {
  return {
    type: HOVER_LINE,
    payload: { lineId }
  };
}

export function deleteLine(lineId: number): ?Action {
  if (confirm(`Do you really want to delete line ${lineId}?`)) {
    return {
      type: DELETE_LINE,
      payload: { lineId }
    };
  }
}

export function updateLine(lineId: number, lineData: LineData): Action {
  return {
    type: UPDATE_LINE,
    payload: { lineId, lineData },
    validations: [
      {
        fn: () => toInteger(lineData["characterId"]) != 0,
        msg: "Line must have a character!"
      },
      {
        fn: () => lineData["text"].length != 0,
        msg: "Line must have text!"
      }
    ]
  };
}

export function createLine(lineData: LineData): Action {
  return {
    type: CREATE_LINE,
    payload: { lineData },
    validations: [
      {
        fn: () => toInteger(lineData["characterId"]) != 0,
        msg: "Line must have a character!"
      },
      {
        fn: () => lineData["text"].length != 0,
        msg: "Line must have text!"
      }
    ]
  };
}
