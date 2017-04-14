import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import store from 'state/store';
import Editor from 'components/editor/component';

ReactDOM.render(
  <Provider store={store}><Editor /></Provider>,
  document.getElementById('root')
);
