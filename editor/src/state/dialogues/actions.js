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

export function updateLine(lineId, line) {
  return {
    type: "UPDATE_LINE",
    payload: { lineId, line }
  };
}
