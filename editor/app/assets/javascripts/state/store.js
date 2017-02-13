import { createStore, applyMiddleware } from 'redux';
import { combineReducers } from 'redux-immutable';
import { fromJS } from 'immutable';
import uiReducer, { initialState as initialUiState } from 'reducers/ui'
import thunk from 'redux-thunk';

let initialState = fromJS({
  ui: initialUiState,
  data: {
    characters: []
  }
})

function dataReducer(state, action) {
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

const reducer = combineReducers({
  ui: uiReducer,
  data: dataReducer
});

export default createStore(reducer, initialState, applyMiddleware(thunk));
