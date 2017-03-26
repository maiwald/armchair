function showNotice(notice) {
  return {
    type: 'SHOW_NOTICE',
    notice
  };
}

function hideNotice() {
  return {
    type: 'HIDE_NOTICE'
  };
}

export function showTimedNotice(notice) {
  return dispatch => {
    dispatch(showNotice(notice));
    setTimeout(() => dispatch(hideNotice()), 4000);
  };
}
