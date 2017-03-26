import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import store from 'state/store';
import { loadCharacters } from 'actions/character_actions';
import Editor from 'components/editor/component'

store.dispatch(loadCharacters());

ReactDOM.render(
  <Provider store={store}><Editor /></Provider>,
  document.getElementById('root')
);
