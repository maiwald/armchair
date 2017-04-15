export function showLineForm(lineId) {
  return {
    type: 'SHOW_LINE_FORM',
    payload: {
      lineId
    }
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
    payload: {
      lineId,
      text
    }
  };
}
