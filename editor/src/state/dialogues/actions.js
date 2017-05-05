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

function sanitizeLineData(lineData) {
  return lineData.update("text", trim).update("characterId", toInteger);
}

export function saveLine(lineId, lineData) {
  const sanitizedLineData = sanitizeLineData(fromJS(lineData));

  return {
    type: "SAVE_LINE",
    payload: { lineId, lineData: sanitizedLineData },
    validations: [
      {
        fn: () => sanitizedLineData.get("characterId") != 0,
        msg: "Line must have a character!"
      },
      {
        fn: () => !isEmpty(sanitizedLineData.get("text")),
        msg: "Line must have text!"
      }
    ]
  };
}
