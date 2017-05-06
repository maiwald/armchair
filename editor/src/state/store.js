import characterReducer from "state/characters/reducer";
import dialogueReducer from "state/dialogues/reducer";
import thunk from "redux-thunk";
import uiReducer from "state/notifications/reducer";
import validationMiddleware from "state/middlewares/validation";
import { combineReducers } from "redux-immutable";
import { createStore, applyMiddleware } from "redux";
import { fromJS } from "immutable";
import { reducer as formReducer } from "redux-form";

const reducer = combineReducers({
  ui: uiReducer,
  characters: characterReducer,
  dialogue: dialogueReducer,
  form: formReducer
});

export default createStore(
  reducer,
  applyMiddleware(thunk, validationMiddleware)
);
