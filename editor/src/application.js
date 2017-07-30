import React from "react";
import ReactDOM from "react-dom";
import { Provider } from "react-redux";
import store from "state/store";
import Editor from "components/editor/component";
import styles from "./application.scss";

import { pressEscape } from "state/notifications/actions";

// hotkey binding
document.addEventListener("keydown", ({ keyCode }) => {
  if (keyCode === 27) {
    store.dispatch(pressEscape());
  }
});

ReactDOM.render(
  <Provider store={store}>
    <Editor />
  </Provider>,
  document.getElementById("root")
);
