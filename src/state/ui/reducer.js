// @flow
import { drop } from "lodash";
import { PUSH_NOTICE, POP_NOTICE } from "state/action_types";
import { getNextId } from "state/utils";

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
          { id: getNextId(state.notifications), text: payload.notice }
        ]
      };

    case POP_NOTICE:
      return { ...state, notifications: drop(state.notifications) };

    default:
      return state;
  }
}
