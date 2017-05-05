import React, { Component } from "react";
import { connect } from "react-redux";
import { isInteger, pick, some } from "lodash";
import { deleteLine, saveLine } from "state/dialogues/actions";
import {
  getEmptyLine,
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

  componentWillReceiveProps({ line }) {
    this.setState({
      text: line.get("text"),
      characterId: line.get("characterId") || ""
    });
  }

  render() {
    const { text } = this.state;
    const { line } = this.props;

    return (
      <div className={[infoBox, styles.lineForm].join(" ")}>
        <header>
          <div className={styles.id}>ID: {line.get("id")}</div>
          <div className={styles.actions}>{this.getDeleteLink()}</div>
        </header>
        <form className={styles.form} onSubmit={this.handleSubmit}>
          <section>
            <label>Character:</label>
            <select
              id="character-select"
              value={this.state.characterId}
              onChange={e => this.setState({ characterId: e.target.value })}
            >
              <option key={null} value="" />
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

  getDeleteLink() {
    const { line, deleteLine } = this.props;

    if (isInteger(line.get("id"))) {
      return (
        <a onClick={() => deleteLine(line.get("id"))}>
          <i className="fa fa-trash-o" /> delete
        </a>
      );
    }
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
    const { line, saveLine } = this.props;
    saveLine(line.get("id"), pick(this.state, ["text", "characterId"]));
  }
}

function mapStateToProps(state) {
  const line = getSelectedLine(state) || getEmptyLine();

  return {
    line,
    characters: getSortedCharacters(state)
  };
}

export default connect(mapStateToProps, { deleteLine, saveLine })(LineForm);
