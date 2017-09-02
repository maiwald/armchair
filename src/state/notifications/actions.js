import { PUSH_NOTICE, POP_NOTICE, PRESS_ESCAPE } from "state/action_types";

function showNotice(notice) {
  return {
    type: PUSH_NOTICE,
    payload: {
      notice
    }
  };
}

function hideNotice() {
  return {
    type: POP_NOTICE
  };
}

export function pressEscape() {
  return {
    type: PRESS_ESCAPE
  };
}

export function showTimedNotice(notice) {
  return dispatch => {
    dispatch(showNotice(notice));
    setTimeout(() => dispatch(hideNotice()), 4000);
  };
}
