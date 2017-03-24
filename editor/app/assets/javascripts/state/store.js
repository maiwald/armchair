import { createStore, applyMiddleware } from 'redux';
import { combineReducers } from 'redux-immutable';
import { fromJS } from 'immutable';
import uiReducer, { initialState as initialUiState } from '../reducers/ui_reducer'
import thunk from 'redux-thunk';

const initialState = fromJS({
  ui: initialUiState,
  data: {
    characters: [],
    dialogue: {
      lines: [
        { id: 1, character: 1, text: "Hey, who are you?", level: 0 },
        { id: 2, text: "I could ask you the same.", level: 1 },
        { id: 3, text: "My name does not matter.", level: 3 },
        { id: 4, character: 1, text: "I am Hugo. And you...?", level: 2 },
        { id: 5, text: "I am Hugo as well.", level: 3 },
        { id: 6, text: "None of your business!", level: 3 },
        { id: 7, character: 1, text: "Fine, be a jerk.", level: 4 },
        { id: 8, character: 1, text: "Nice to meet you!", level: 4 },
        { id: 9, character: 1, text: "Ok, bye!", level: 5 }
      ],
      connections: [
        { from: 1, to: 2 },
        { from: 2, to: 4 },
        { from: 4, to: 5 },
        { from: 4, to: 6 },
        { from: 6, to: 7 },
        { from: 5, to: 8 },
        { from: 8, to: 9 },
        { from: 1, to: 3 },
        { from: 3, to: 7 },
        { from: 7, to: 9 },
      ]
    },
  }
});

function dataReducer(state, action) {
  switch (action.type) {
    case 'RESET_CHARACTERS':
      return state.set('characters', fromJS(action.characters));

    case 'ADD_CHARACTER':
      return state.update('characters', characters => {
        return characters.push(fromJS(action.character));
      });

    case 'UPDATE_LINE':
      const { lineId, text } = action;
      const lineIndex = state.getIn(['dialogue', 'lines']).findIndex(line => {
        return line.get('id') == lineId;
      });
      return state.setIn(['dialogue', 'lines', lineIndex, 'text'], text);

    default:
      return state;
  }
}

const reducer = combineReducers({
  ui: uiReducer,
  data: dataReducer
});

export default createStore(reducer, initialState, applyMiddleware(thunk));
