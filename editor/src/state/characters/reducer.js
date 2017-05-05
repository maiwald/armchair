import { Map, fromJS } from "immutable";

const initialState = fromJS([
  { id: 0, name: "Player" },
  { id: 1, name: "Hugo" },
  { id: 2, name: "Dende" }
]);

function getNextCharacterId(state) {
  return state.map(c => c.get("id")).max() + 1;
}

export default function characterReducer(
  state = initialState,
  { type, payload }
) {
  switch (type) {
    case "RESET_CHARACTERS":
      return fromJS(payload.characters);

    case "CREATE_CHARACTER":
      return state.push(
        new Map({ id: getNextCharacterId(state), name: payload.name })
      );

    default:
      return state;
  }
}
