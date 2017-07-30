// @flow
import { isEmpty, trim, toInteger } from "lodash";
import { fromJS } from "immutable";

import {
  SELECT_LINE,
  SET_MODAL_SELECTION,
  DELETE_LINE,
  UPDATE_LINE,
  CREATE_LINE
} from "state/action_types";

export function setModalSelection(value: boolean) {
  return {
    type: SET_MODAL_SELECTION,
    payload: value
  };
}

export function selectLine(lineId: number) {
  return {
    type: SELECT_LINE,
    payload: { lineId }
  };
}

export function deleteLine(lineId: number) {
  return {
    type: DELETE_LINE,
    payload: { lineId }
  };
}

export function updateLine(lineId: number, lineData: any) {
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

export function createLine(lineData: any) {
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
