import React, { Component } from "react";
import { connect } from "react-redux";
import { isEqual, isInteger, pick } from "lodash";
import { createLine, updateLine, deleteLine } from "state/dialogues/actions";
import {
  getEmptyLine,
  getSelectedLine,
  getOutboundLines
} from "state/dialogues/selectors";
import { getSortedCharacters } from "state/characters/selectors";
import styles from "./styles.scss";
import { infoBox } from "shared_styles/info_box.scss";
import OutboundLines from "./outbound_lines";

function handleStateUpdate(self, name) {
  return e => {
    const value = e.target.value;
    self.setState({ [name]: value });
  };
}

class LineForm extends Component {
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
    const { lineId } = this.props;
    const pristine = this.isPristine();

    return (
      <div className={[infoBox, styles.lineForm].join(" ")}>
        <header>
          <div className={styles.id}>
            ID: {lineId}
          </div>
          <div className={styles.actions}>
            {this.getDeleteLink()}
          </div>
        </header>
        <form className={styles.form} onSubmit={this.onSubmit.bind(this)}>
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

          <section>
            <OutboundLines lines={this.props.outboundLines} />
          </section>

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
        </form>
      </div>
    );
  }

  getDeleteLink() {
    const { lineId, deleteLine } = this.props;

    if (isInteger(lineId)) {
      return (
        <a onClick={() => deleteLine(lineId)}>
          <i className="fa fa-trash-o" /> delete
        </a>
      );
    }
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
    outboundLines: getOutboundLines(state, line.get("id")),
    initialValues: {
      text: line.get("text"),
      characterId: line.get("characterId")
    },
    lineId: line.get("id")
  };
}

export default connect(mapStateToProps, { createLine, updateLine, deleteLine })(
  LineForm
);
