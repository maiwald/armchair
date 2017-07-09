import characterReducer from "state/characters/reducer";
import dialogueReducer from "state/dialogues/reducer";
import thunk from "redux-thunk";
import uiReducer from "state/notifications/reducer";
import validationMiddleware from "state/middlewares/validation";
import { combineReducers } from "redux-immutable";
import { createStore, applyMiddleware } from "redux";
import { fromJS } from "immutable";

const reducer = combineReducers({
  ui: uiReducer,
  characters: characterReducer,
  dialogue: dialogueReducer
});

export default createStore(
  reducer,
  applyMiddleware(thunk, validationMiddleware)
);
