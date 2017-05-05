function showNotice(notice) {
  return {
    type: "PUSH_NOTICE",
    payload: {
      notice
    }
  };
}

function hideNotice() {
  return {
    type: "POP_NOTICE"
  };
}

export function showTimedNotice(notice) {
  return dispatch => {
    dispatch(showNotice(notice));
    setTimeout(() => dispatch(hideNotice()), 4000);
  };
}
