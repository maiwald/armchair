// @flow
import React, { Component } from "react";
import { connect } from "react-redux";
import Characters from "components/characters/component";
import { createLine, updateLine } from "state/dialogues/actions";
import Dialogue from "components/dialogue/component";
import LineForm from "components/line_form/component";
import Notifications from "./notifications";
import { getSelectedLine } from "state/dialogues/selectors";
import { getSortedCharacters } from "state/characters/selectors";
import styles from "./styles.css";

class Editor extends Component {
  render() {
    return (
      <div>
        <div className={styles.notifications}>
          <Notifications />
        </div>

        <div className={styles.menu}>
          <Characters />
        </div>

        <div className={styles.container}>
          <div className={styles.canvas}>
            <Dialogue />
          </div>

          <div className={styles.form}>{this.getLineForm()}</div>
        </div>
      </div>
    );
  }

  getLineForm() {
    const { selectedLine: line } = this.props;

    if (line != null) {
      return (
        <LineForm
          characters={this.props.characters}
          handleSubmit={this.props.updateLine}
          lineId={line.id}
          initialValues={{
            text: line.text,
            characterId: line.characterId
          }}
        />
      );
    }
  }
}

function mapStateToProps(state) {
  return {
    characters: getSortedCharacters(state),
    selectedLine: getSelectedLine(state)
  };
}

export default connect(mapStateToProps, { createLine, updateLine })(Editor);
