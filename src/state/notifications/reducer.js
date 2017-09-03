// @flow
import { drop } from "lodash";
import { PUSH_NOTICE, POP_NOTICE } from "state/action_types";

const initialState: NotificationState = {
  notifications: []
};

export default function reducer(
  state: NotificationState = initialState,
  { type, payload }: Action
): NotificationState {
  switch (type) {
    case PUSH_NOTICE:
      return {
        ...state,
        notifications: [
          ...state.notifications,
          { id: Date.now(), text: payload.notice }
        ]
      };

    case POP_NOTICE:
      return { ...state, notifications: drop(state.notifications) };

    default:
      return state;
  }
}
