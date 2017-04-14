import { createStore, applyMiddleware } from 'redux';
import { combineReducers } from 'redux-immutable';
import { fromJS } from 'immutable';
import uiReducer, { initialState as initialUiState } from 'reducers/ui_reducer';
import dialogueReducer, {
  initialState as initialDialogueState
} from 'reducers/dialogue_reducer';
import characterReducer, {
  initialState as initialCharactersState
} from 'reducers/character_reducer';
import thunk from 'redux-thunk';

const initialState = fromJS({
  ui: initialUiState,
  characters: initialCharactersState,
  dialogue: initialDialogueState
});

const reducer = combineReducers({
  ui: uiReducer,
  characters: characterReducer,
  dialogue: dialogueReducer
});

export default createStore(reducer, initialState, applyMiddleware(thunk));
