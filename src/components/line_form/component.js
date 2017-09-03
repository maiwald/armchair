// @flow
import React, { Component } from "react";
import { connect } from "react-redux";
import { isUndefined, isEqual, isInteger, pick } from "lodash";
import typeof { updateLine } from "state/dialogues/actions";
import { getSelectedLine } from "state/dialogues/selectors";
import { getSortedCharacters } from "state/characters/selectors";
import styles from "./styles.scss";

function handleStateUpdate(self: LineForm, name: string) {
  return (e: SyntheticInputEvent) => {
    const value = e.target.value;
    self.setState({ [name]: value });
  };
}

type ValueProps = {
  characters: Character[],
  initialValues: {
    text: string,
    characterId: number
  },
  lineId: number
};

type DispatchProps = {
  handleSubmit: updateLine
};

type Props = ValueProps & DispatchProps;

export default class LineForm extends Component {
  state: {
    text: string,
    characterId: number,
    initialValues: {
      text: string,
      characterId: number
    }
  };
  props: Props;

  constructor(props: Props) {
    super(props);

    this.state = {
      ...props.initialValues,
      initialValues: props.initialValues
    };
  }

  componentWillReceiveProps({ initialValues }: Props) {
    if (!isEqual(this.props.initialValues, initialValues)) {
      this.setState({ ...initialValues, initialValues });
    }
  }

  render() {
    const { lineId } = this.props;
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
        <option key={c.id} value={c.id}>
          {c.name}
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

  onSubmit(e: SyntheticEvent) {
    e.preventDefault();

    const { lineId, handleSubmit } = this.props;
    const data = this.getLineData();

    handleSubmit(lineId, data);
  }
}
