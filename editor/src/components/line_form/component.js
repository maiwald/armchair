import React, { Component } from "react";
import { connect } from "react-redux";
import { toInteger, trim, isUndefined, pick } from "lodash";
import { deleteLine, updateLine } from "state/dialogues/actions";
import {
  getSelectedLine,
  getLineFormPosition
} from "state/dialogues/selectors";
import { getCharacter, getSortedCharacters } from "state/characters/selectors";
import styles from "./styles.scss";
import { infoBox } from "shared_styles/info_box.scss";

class LineForm extends Component {
  constructor(props) {
    super(props);

    this.state = { text: "", characterId: "" };
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentWillReceiveProps({ line, character }) {
    this.setState({
      text: line.get("text"),
      characterId: character.get("id")
    });
  }

  render() {
    const { text } = this.state;
    const { line, character, deleteLine } = this.props;

    return (
      <div className={[infoBox, styles.lineForm].join(" ")}>
        <header>
          <div className={styles.id}>ID: {line.get("id")}</div>
          <div className={styles.actions}>
            <a onClick={() => deleteLine(line.get("id"))}>
              <i className="fa fa-trash-o" /> delete
            </a>
          </div>
        </header>
        <form className={styles.form} onSubmit={this.handleSubmit}>
          <section>
            <label>Character:</label>
            <select
              id="character-select"
              value={this.state.characterId}
              onChange={e => this.setState({ characterId: e.target.value })}
            >
              {this.getCharacterOptions()}
            </select>
          </section>

          <section>
            <label>Text:</label>
            <textarea
              onChange={e => this.setState({ text: e.target.value })}
              value={this.state.text}
            />
          </section>
          <button type="submit">Save</button>
        </form>
      </div>
    );
  }

  getCharacterOptions() {
    return this.props.characters.map(c => {
      return (
        <option key={c.get("id")} value={c.get("id")}>{c.get("name")}</option>
      );
    });
  }

  handleSubmit(event) {
    event.preventDefault();

    const { line, updateLine } = this.props;
    const { text, characterId } = this.state;

    updateLine(
      line.get("id"),
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
    character = getCharacter(state, line.get("characterId"));
  }

  return {
    line,
    character,
    characters: getSortedCharacters(state)
  };
}

export default connect(mapStateToProps, { deleteLine, updateLine })(
  FormWrapper
);
