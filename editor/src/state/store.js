import { createStore, applyMiddleware } from "redux";
import { combineReducers } from "redux-immutable";
import { fromJS } from "immutable";
import thunk from "redux-thunk";
import uiReducer from "state/notifications/reducer";
import dialogueReducer from "state/dialogues/reducer";
import characterReducer from "state/characters/reducer";
import validationMiddleware from "state/middlewares/validation";

const reducer = combineReducers({
  ui: uiReducer,
  characters: characterReducer,
  dialogue: dialogueReducer
});

export default createStore(
  reducer,
  applyMiddleware(thunk, validationMiddleware)
);
