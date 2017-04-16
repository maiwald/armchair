import React, { Component } from 'react';
import { connect } from 'react-redux';
import { toInteger, trim, isUndefined, pick } from 'lodash';
import { updateLine } from 'state/dialogues/actions';
import {
  getSelectedLine,
  getLineFormPosition
} from 'state/dialogues/selectors';
import { getCharacter, getSortedCharacters } from 'state/characters/selectors';
import styles from './styles.scss';

class LineForm extends Component {
  constructor(props) {
    super(props);

    this.state = { text: '', characterId: '' };
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentWillReceiveProps({ line, character }) {
    this.setState({
      text: line.get('text'),
      characterId: character.get('id')
    });
  }

  render() {
    const { text } = this.state;
    const { line, character, position } = this.props;

    return (
      <div style={this.getFormPositionCss()} className={styles.wrapper}>
        <header>ID: {line.get('id')}</header>
        <form className={styles.form} onSubmit={this.handleSubmit}>
          Character:
          <select
            value={this.state.characterId}
            onChange={e => this.setState({ characterId: e.target.value })}
          >
            {this.getCharacterOptions()}
          </select>
          <textarea
            className={styles.text}
            onChange={e => this.setState({ text: e.target.value })}
            value={this.state.text}
          />
          <button type="submit">Save</button>
        </form>
      </div>
    );
  }

  getFormPositionCss() {
    const { position } = this.props;

    return {
      top: position.get('y'),
      left: position.get('x')
    };
  }

  getCharacterOptions() {
    return this.props.characters.map(c => {
      return (
        <option key={c.get('id')} value={c.get('id')}>{c.get('name')}</option>
      );
    });
  }

  handleSubmit(event) {
    event.preventDefault();

    const { line, updateLine } = this.props;
    const { text, characterId } = this.state;

    updateLine(
      line.get('id'),
      line.merge({
        text: trim(text),
        characterId: toInteger(characterId)
      })
    );
  }
}

function FormWrapper(props) {
  if (isUndefined(props.line)) {
    return null;
  } else {
    return <LineForm {...props} />;
  }
}

function mapStateToProps(state) {
  const line = getSelectedLine(state);
  let character;

  if (!isUndefined(line)) {
    character = getCharacter(state, line.get('characterId'));
  }

  return {
    line,
    character,
    characters: getSortedCharacters(state),
    position: getLineFormPosition(state)
  };
}

export default connect(mapStateToProps, { updateLine })(FormWrapper);
