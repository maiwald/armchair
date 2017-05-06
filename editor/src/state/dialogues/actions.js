import { isEmpty, trim, toInteger } from "lodash";
import { fromJS } from "immutable";

export function showLineForm(lineId) {
  return {
    type: "SHOW_LINE_FORM",
    payload: { lineId }
  };
}

export function hideLineForm() {
  return {
    type: "HIDE_LINE_FORM"
  };
}

export function deleteLine(lineId) {
  return {
    type: "DELETE_LINE",
    payload: { lineId }
  };
}

export function updateLine(lineId, lineData) {
  return {
    type: "UPDATE_LINE",
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
    type: "CREATE_LINE",
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
