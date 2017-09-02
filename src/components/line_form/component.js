// @flow

import React, { Component } from "react";
import { connect } from "react-redux";
import { isEqual, isInteger, pick } from "lodash";
import {
  createLine,
  updateLine,
  setSelectionMode
} from "state/dialogues/actions";
import { getEmptyLine, getSelectedLine } from "state/dialogues/selectors";
import { getSortedCharacters } from "state/characters/selectors";
import styles from "./styles.scss";

function handleStateUpdate(self, name) {
  return e => {
    const value = e.target.value;
    self.setState({ [name]: value });
  };
}

class LineForm extends Component {
  state: any;

  constructor(props) {
    super(props);

    this.state = {
      ...props.initialValues,
      initialValues: props.initialValues
    };
  }

  componentWillReceiveProps({ initialValues }) {
    if (!isEqual(this.props.initialValues, initialValues)) {
      this.setState({ ...initialValues, initialValues });
    }
  }

  render() {
    const { lineId, setSelectionMode } = this.props;
    const pristine = this.isPristine();

    return (
      <form className={styles.container} onSubmit={this.onSubmit.bind(this)}>
        <header>Line ID: {lineId}</header>

        <div className={styles.form}>
          <section>
            <label>Character:</label>
            <select
              value={this.state.characterId || ""}
              onChange={handleStateUpdate(this, "characterId")}
            >
              <option key="" />
              {this.getCharacterOptions()}
            </select>
          </section>

          <section>
            <label>Text:</label>
            <textarea
              value={this.state.text}
              onChange={handleStateUpdate(this, "text")}
            />
          </section>
        </div>

        <div className={styles.actions}>
          <button type="submit" disabled={pristine}>
            Save
          </button>
          <button
            type="button"
            disabled={pristine}
            onClick={this.reset.bind(this)}
          >
            Reset
          </button>
        </div>
      </form>
    );
  }

  getCharacterOptions() {
    return this.props.characters.map(c => {
      return (
        <option key={c.get("id")} value={c.get("id")}>
          {c.get("name")}
        </option>
      );
    });
  }

  isPristine() {
    return isEqual(this.getLineData(), this.state.initialValues);
  }

  reset() {
    this.setState(this.state.initialValues);
  }

  getLineData() {
    return pick(this.state, ["characterId", "text"]);
  }

  onSubmit(e) {
    e.preventDefault();

    const { lineId, createLine, updateLine } = this.props;
    const data = this.getLineData();

    if (isInteger(lineId)) {
      updateLine(lineId, data);
    } else {
      createLine(data);
    }
  }
}

function mapStateToProps(state) {
  const line = getSelectedLine(state) || getEmptyLine();

  return {
    characters: getSortedCharacters(state),
    initialValues: {
      text: line.get("text"),
      characterId: line.get("characterId")
    },
    lineId: line.get("id")
  };
}

export default connect(mapStateToProps, {
  createLine,
  updateLine,
  setSelectionMode
})(LineForm);
