import React, { Component } from 'react';
import { connect } from 'react-redux';
import { trim, isUndefined, pick } from 'lodash';
import { updateLine } from 'state/dialogues/actions';
import {
  getSelectedLine,
  getLineFormPosition
} from 'state/dialogues/selectors';
import { getCharacter } from 'state/characters/selectors';
import styles from './styles.scss';

class LineForm extends Component {
  constructor(props) {
    super(props);

    this.state = { text: '' };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentWillReceiveProps({ line }) {
    this.setState({ text: line.get('text') });
  }

  render() {
    const { text } = this.state;
    const { line, character, position } = this.props;

    return (
      <div
        style={{
          position: 'absolute',
          top: position.get('y'),
          left: position.get('x')
        }}
        className={styles.wrapper}
      >
        <header>ID: {line.get('id')}</header>
        <p>{character.get('name')}</p>
        <form className={styles.form} onSubmit={this.handleSubmit}>
          <textarea
            className={styles.text}
            onChange={this.handleChange}
            value={this.state.text}
          />
          <button type="submit">Save</button>
        </form>
        <section className={styles.connections}>
          <p>Inbound connections</p>
          <ul>
            <li>some inbound line <a>x</a></li>
            <li>some inbound line <a>x</a></li>
          </ul>
        </section>
      </div>
    );
  }

  handleChange(event) {
    this.setState({ text: event.target.value });
  }

  handleSubmit(event) {
    event.preventDefault();
    const { line } = this.props;
    this.props.updateLine(line.get('id'), trim(this.state.text));
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
    position: getLineFormPosition(state)
  };
}

export default connect(mapStateToProps, { updateLine })(FormWrapper);
