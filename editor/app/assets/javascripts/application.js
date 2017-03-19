import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import store from './state/store';
import { loadCharacters } from './actions/character';
import CharacterForm from './components/character_form';
import CharacterList from './components/character_list';
import Notice from './components/notice';

store.dispatch(loadCharacters());

function Editor() {
  return (
    <div>
      <Notice />
      <h1>Hello!</h1>
      <CharacterList />
      <CharacterForm />
    </div>
  );
}

ReactDOM.render(
  <Provider store={store}><Editor /></Provider>,
  document.getElementById('root')
);
