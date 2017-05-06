import React, { Component } from "react";
import { connect } from "react-redux";
import { isInteger, pick } from "lodash";
import { createLine, updateLine, deleteLine } from "state/dialogues/actions";
import { getEmptyLine, getSelectedLine } from "state/dialogues/selectors";
import { getSortedCharacters } from "state/characters/selectors";
import styles from "./styles.scss";
import { infoBox } from "shared_styles/info_box.scss";
import { Field } from "redux-form";
import { reduxForm } from "redux-form/immutable";
import { fromJS } from "immutable";

function getLineData(line) {
  return {
    text: line.get("text"),
    characterId: line.get("characterId")
  };
}

class LineForm extends Component {
  componentWillReceiveProps({ line }) {
    if (this.props.line.get("id") != line.get("id")) {
      this.props.initialize(getLineData(line));
    }
  }

  render() {
    const { line, handleSubmit, reset, pristine } = this.props;

    return (
      <div className={[infoBox, styles.lineForm].join(" ")}>
        <header>
          <div className={styles.id}>ID: {line.get("id")}</div>
          <div className={styles.actions}>{this.getDeleteLink()}</div>
        </header>
        <form
          className={styles.form}
          onSubmit={handleSubmit(this.onSubmit.bind(this))}
        >
          <section>
            <label>Character:</label>
            <Field name="characterId" component="select">
              <option key={null} />
              {this.getCharacterOptions()}
            </Field>
          </section>

          <section>
            <label>Text:</label>
            <Field name="text" component="textarea" />
          </section>
          <button type="submit" disabled={pristine}>Save</button>
          <button type="button" disabled={pristine} onClick={reset}>
            Reset
          </button>
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

  onSubmit(data) {
    const { line, createLine, updateLine } = this.props;
    if (isInteger(line.get("id"))) {
      updateLine(line.get("id"), data);
    } else {
      createLine(data);
    }
  }
}

function mapStateToProps(state) {
  const line = getSelectedLine(state) || getEmptyLine();

  return {
    line,
    characters: getSortedCharacters(state)
  };
}

export default connect(mapStateToProps, { createLine, updateLine, deleteLine })(
  reduxForm({ form: "lineForm" })(LineForm)
);
