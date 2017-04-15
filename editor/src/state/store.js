import { createStore, applyMiddleware } from 'redux';
import { combineReducers } from 'redux-immutable';
import { fromJS } from 'immutable';
import uiReducer from 'reducers/ui_reducer';
import dialogueReducer from 'reducers/dialogue_reducer';
import characterReducer from 'reducers/character_reducer';
import thunk from 'redux-thunk';

const reducer = combineReducers({
  ui: uiReducer,
  characters: characterReducer,
  dialogue: dialogueReducer
});

export default createStore(reducer, applyMiddleware(thunk));
