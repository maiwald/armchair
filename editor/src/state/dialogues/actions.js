export function showLineForm(lineId) {
  return {
    type: 'SHOW_LINE_FORM',
    payload: { lineId }
  };
}

export function setLineFormPosition({ x, y }) {
  return {
    type: 'SET_LINE_FORM_POSITION',
    payload: { x, y }
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
    payload: { lineId, text }
  };
}
