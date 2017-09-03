// @flow
import { max } from "lodash";
import { RESET_CHARACTERS, CREATE_CHARACTER } from "state/action_types";

const initialState: CharacterState = [
  { id: 1, name: "Hugo" },
  { id: 2, name: "Player" },
  { id: 3, name: "Dende" }
];

function getNextCharacterId(state: CharacterState): number {
  return max(state.map(c => c.id)) + 1;
}

export default function characterReducer(
  state: CharacterState = initialState,
  { type, payload }: Action
): CharacterState {
  switch (type) {
    case RESET_CHARACTERS:
      return payload.characters;

    case CREATE_CHARACTER:
      return [...state, { id: getNextCharacterId(state), name: payload.name }];

    default:
      return state;
  }
}
