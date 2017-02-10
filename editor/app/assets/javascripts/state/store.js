import { createStore, applyMiddleware } from 'redux';
import { fromJS } from 'immutable';
import thunk from 'redux-thunk';

let initialState = fromJS({
  characters: []
})

function reducer(state, action) {
  switch (action.type) {
    case 'RESET_CHARACTERS':
      return state.set('characters', fromJS(action.characters));

    case 'ADD_CHARACTER':
      return state.update('characters', characters => {
        return characters.push(fromJS(action.character));
      });

    default:
      return state;
  }
}

export default createStore(reducer, initialState, applyMiddleware(thunk));
