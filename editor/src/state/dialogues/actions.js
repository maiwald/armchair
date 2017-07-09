import { isEmpty, trim, toInteger } from "lodash";
import { fromJS } from "immutable";

import {
  SET_SELECTED_LINE,
  DELETE_LINE,
  UPDATE_LINE,
  CREATE_LINE
} from "state/action_types";

export function setSelectedLine(lineId) {
  return {
    type: SET_SELECTED_LINE,
    payload: { lineId }
  };
}

export function deleteLine(lineId) {
  return {
    type: DELETE_LINE,
    payload: { lineId }
  };
}

export function updateLine(lineId, lineData) {
  return {
    type: UPDATE_LINE,
    payload: { lineId, lineData },
    validations: [
      {
        fn: () => toInteger(lineData["characterId"]) != 0,
        msg: "Line must have a character!"
      },
      {
        fn: () => !isEmpty(lineData["text"]),
        msg: "Line must have text!"
      }
    ]
  };
}

export function createLine(lineData) {
  return {
    type: CREATE_LINE,
    payload: { lineData },
    validations: [
      {
        fn: () => toInteger(lineData["characterId"]) != 0,
        msg: "Line must have a character!"
      },
      {
        fn: () => !isEmpty(lineData["text"]),
        msg: "Line must have text!"
      }
    ]
  };
}
