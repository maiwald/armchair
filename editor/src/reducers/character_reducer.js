import { Map, fromJS } from 'immutable';

const initialState = fromJS([
  { id: 1, name: 'Hugo' },
  { id: 2, name: 'Dende' }
]);

export function selectCharacters(state) {
  return state
    .get('characters')
    .sortBy(c => c.get('name'), (lhs, rhs) => lhs.localeCompare(rhs));
}

export function selectCharacterNames(state) {
  return state.get('characters').map(c => c.get('name'));
}

function getNextCharacterId(state) {
  return state.map(c => c.get('id')).max() + 1;
}

export default function characterReducer(
  state = initialState,
  { type, payload }
) {
  switch (type) {
    case 'RESET_CHARACTERS':
      return fromJS(payload.characters);

    case 'CREATE_CHARACTER':
      return state.push(
        new Map({ id: getNextCharacterId(state), name: payload.name })
      );

    default:
      return state;
  }
}
