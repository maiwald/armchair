import { Map, fromJS } from "immutable";

import { RESET_CHARACTERS, CREATE_CHARACTER } from "state/action_types";

const initialState = fromJS([
  { id: 1, name: "Hugo" },
  { id: 2, name: "Player" },
  { id: 3, name: "Dende" }
]);

function getNextCharacterId(state) {
  return state.map(c => c.get("id")).max() + 1;
}

export default function characterReducer(
  state = initialState,
  { type, payload }
) {
  switch (type) {
    case RESET_CHARACTERS:
      return fromJS(payload.characters);

    case CREATE_CHARACTER:
      return state.push(
        new Map({ id: getNextCharacterId(state), name: payload.name })
      );

    default:
      return state;
  }
}
