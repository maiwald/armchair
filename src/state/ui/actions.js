// @flow
import { PUSH_NOTICE, POP_NOTICE, PRESS_ESCAPE } from "state/action_types";

function showNotice(notice: string): Action {
  return {
    type: PUSH_NOTICE,
    payload: {
      notice
    }
  };
}

function hideNotice(): Action {
  return {
    type: POP_NOTICE,
    payload: null
  };
}

export function pressEscape(): Action {
  return {
    type: PRESS_ESCAPE,
    payload: null
  };
}

export function showTimedNotice(notice: string): ActionThunk {
  return dispatch => {
    dispatch(showNotice(notice));
    setTimeout(() => dispatch(hideNotice()), 4000);
  };
}
