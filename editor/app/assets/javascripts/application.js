import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import { Provider, connect } from 'react-redux';
import { isNull } from 'lodash';
import store from 'state/store';
import { loadCharacters, createCharacter } from 'actions/character'

store.dispatch(loadCharacters());

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
    event.preventDefault();
    this.props.createCharacter(this.state.name);
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

const ConnectedCharacterForm = connect(null, {createCharacter})(CharacterForm);

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
  return { characters: state.getIn(['data', 'characters']).toJS() };
})(CharacterList);


const Notice = ({notice}) => {
  return isNull(notice) ? null : <div>{notice}</div>;
}

const ConnectedNotice = connect(state => {
  return { notice: state.getIn(['ui', 'notice']) };
})(Notice);


const Editor = () => {
  return (
    <div>
      <ConnectedNotice />
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
