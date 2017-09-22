// @flow
import { drop } from "lodash";
import { PUSH_NOTICE, POP_NOTICE } from "state/action_types";

const initialState: UiState = {
  notifications: []
};

export default function reducer(
  state: UiState = initialState,
  { type, payload }: Action
): UiState {
  switch (type) {
    case PUSH_NOTICE:
      return {
        ...state,
        notifications: [
          ...state.notifications,
          { id: new Date(), text: payload.notice }
        ]
      };

    case POP_NOTICE:
      return { ...state, notifications: drop(state.notifications) };

    default:
      return state;
  }
}
