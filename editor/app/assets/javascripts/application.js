import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import { Provider, connect } from 'react-redux';
import { getJSON, postJSON } from 'ajax_helpers';
import store from 'state/store';

function resetCharacters(characters) {
  return {
    type: 'RESET_CHARACTERS',
    characters
  };
}

function addCharacter(character) {
  return {
    type: 'ADD_CHARACTER',
    character
  };
}

function loadCharacters() {
  store.dispatch(dispatch => {
    getJSON('/characters.json')
      .then(characters => dispatch(resetCharacters(characters)))
      .catch(e => { console.log(e); });
  });
}

function createCharacter(name) {
  return dispatch => {
    postJSON('/characters.json', { name: name })
      .then(character => dispatch(addCharacter(character)))
      .catch(e => { console.log(e); });
  };
}

loadCharacters();

class CharacterForm extends Component {

  constructor(props) {
    super(props);
    this.state = { name: '' };

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleChange(event) {
    this.setState({name: event.target.value});
  }

  handleSubmit(event) {
    if (this.state.name.length != 0) {
      this.props.dispatch(createCharacter(this.state.name));
    }
    event.preventDefault();
  }

  render() {
    return (
      <form onSubmit={this.handleSubmit}>
        <label>
          <input onChange={this.handleChange} id='name' type='text' value={this.state.name} />
        </label>
        <button type='submit'>Create character</button>
      </form>
    );
  }
}

const CharacterItem = ({character}) => {
  return (
    <li>{character.name}</li>
  );
}

const CharacterList = ({characters}) => {
  let items = characters.map((c) => {
    return <CharacterItem key={c.id} character={c} />;
  });

  return (
    <ul>{items}</ul>
  );
}

const ConnectedCharacterList = connect(state => {
  return { characters: state.get('characters').toJS() };
})(CharacterList);

const ConnectedCharacterForm = connect()(CharacterForm);

const Editor = () => {
  return (
    <div>
      <h1>Hello!</h1>
      <ConnectedCharacterList />
      <ConnectedCharacterForm />
    </div>
  );
}

ReactDOM.render(
  <Provider store={store}><Editor /></Provider>,
  document.getElementById('root')
);
