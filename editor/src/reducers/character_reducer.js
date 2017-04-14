import { Map, fromJS } from 'immutable';

export const initialState = fromJS([
  { id: 1, name: 'Hugo' },
  { id: 2, name: 'Dende' }
]);

export function getCharacters(state) {
  return state
    .get('characters')
    .sortBy(c => c.get('name'), (lhs, rhs) => lhs.localeCompare(rhs));
}

function getNextCharacterId(state) {
  return state.map(c => c.get('id')).max() + 1;
}

export default function characterReducer(state, action) {
  switch (action.type) {
    case 'RESET_CHARACTERS':
      return fromJS(action.characters);

    case 'CREATE_CHARACTER':
      return state.push(
        new Map({ id: getNextCharacterId(state), name: action.name })
      );

    default:
      return state;
  }
}
