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
import styles from "./styles.scss";

type Props = {
  selectedLine: ?Line,
  createLine: typeof createLine,
  updateLine: typeof updateLine
};

type State = {
  showLineCreationModal: boolean
};

class Editor extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { showLineCreationModal: false };
  }

  showLineCreationModal() {
    this.setState({ showLineCreationModal: true });
  }

  hideLineCreationModal() {
    this.setState({ showLineCreationModal: false });
  }

  render() {
    return (
      <div>
        {this.getLineCreationModal()}
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
      return <LineForm onSubmit={this.props.updateLine} line={line} />;
    } else {
      return <a onClick={this.showLineCreationModal.bind(this)}>New Line</a>;
    }
  }

  getLineCreationModal() {
    if (this.state.showLineCreationModal) {
      return (
        <div className={styles.modal}>
          <div>
            <LineForm
              onSubmit={(lineData: LineData) => {
                this.props.createLine(lineData);
                this.hideLineCreationModal();
              }}
              onCancel={this.hideLineCreationModal.bind(this)}
              line={null}
            />
          </div>
        </div>
      );
    }
  }
}

function mapStateToProps(state) {
  return {
    selectedLine: getSelectedLine(state)
  };
}

export default connect(mapStateToProps, {
  createLine,
  updateLine
})(Editor);
