import { fromJS } from 'immutable';

export default function characterReducer(state, action) {
  switch (action.type) {
    case 'RESET_CHARACTERS':
      return fromJS(action.characters);

    case 'ADD_CHARACTER':
      return state.push(fromJS(action.character));

    default:
      return state;
  }
}
