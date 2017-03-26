export function showLineForm(lineId) {
  return {
    type: 'SHOW_LINE_FORM',
    lineId
  };
}

export function hideLineForm() {
  return {
    type: 'HIDE_LINE_FORM'
  };
}

export function updateLine(lineId, text) {
  return {
    type: 'UPDATE_LINE',
    lineId,
    text
  };
}
