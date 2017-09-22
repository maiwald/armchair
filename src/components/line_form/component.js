// @flow
import React, { Component } from "react";
import { connect } from "react-redux";
import { isUndefined, isEqual, isInteger, pick } from "lodash";
import typeof { updateLine } from "state/dialogues/actions";
import { getSelectedLine } from "state/dialogues/selectors";
import { getSortedCharacters } from "state/characters/selectors";
import styles from "./styles.scss";

function handleInputChange(self: LineForm, name: string) {
  return (e: SyntheticInputEvent<>) => {
    const value = e.target.value;
    self.setState({ [name]: value });
  };
}

function getLineData(line: ?Line | State): LineData {
  if (line) {
    return {
      characterId: line.characterId,
      text: line.text
    };
  } else {
    return {
      characterId: null,
      text: ""
    };
  }
}

type State = LineData & {
  initialValues: LineData
};

type ComponentProps = {
  characters: Character[],
  line: ?Line,
  onCancel?: void => void,
  onSubmit: LineData => void
};

class LineForm extends Component<ComponentProps, State> {
  constructor(props: ComponentProps) {
    super(props);

    const lineData = getLineData(props.line);
    this.state = { ...lineData, initialValues: lineData };
  }

  componentWillReceiveProps({ line }: ComponentProps) {
    const lineData = getLineData(line);

    if (!isEqual(getLineData(this.props.line), lineData)) {
      this.setState({ ...lineData, initialValues: lineData });
    }
  }

  render() {
    const { line } = this.props;
    const pristine = this.isPristine();

    return (
      <form
        className={styles.container}
        onSubmit={this.handleSubmit.bind(this)}
      >
        <header>Line ID: {line ? line.id : "XXX"}</header>

        <div className={styles.form}>
          <section>
            <label>Character:</label>
            <select
              value={this.state.characterId || ""}
              onChange={handleInputChange(this, "characterId")}
            >
              <option key="" />
              {this.getCharacterOptions()}
            </select>
          </section>

          <section>
            <label>Text:</label>
            <textarea
              value={this.state.text}
              onChange={handleInputChange(this, "text")}
            />
          </section>
        </div>

        <div className={styles.actions}>
          <button type="submit" disabled={pristine}>
            Save
          </button>
          <button
            type="button"
            disabled={!this.props.onCancel && pristine}
            onClick={this.handleCancel.bind(this)}
          >
            Cancel
          </button>
        </div>
      </form>
    );
  }

  getCharacterOptions() {
    return this.props.characters.map(c => {
      return (
        <option key={c.id} value={c.id}>
          {c.name}
        </option>
      );
    });
  }

  isPristine() {
    return isEqual(getLineData(this.state), this.state.initialValues);
  }

  handleCancel() {
    const { onCancel } = this.props;
    if (onCancel) {
      onCancel();
    } else {
      this.setState(this.state.initialValues);
    }
  }

  handleSubmit(e: SyntheticEvent<>) {
    e.preventDefault();
    this.props.onSubmit(getLineData(this.state));
  }
}

export default connect(state => {
  return {
    characters: getSortedCharacters(state)
  };
}, {})(LineForm);
