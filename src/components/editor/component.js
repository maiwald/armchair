import React from "react";
import { connect } from "react-redux";
import Characters from "components/characters/component";
import Dialogue from "components/dialogue/component";
import LineForm from "components/line_form/component";
import Notifications from "components/notifications/component";
import { hasSelectedLine } from "state/dialogues/selectors";
import styles from "./styles.css";

function Editor(props) {
  const formComponent = props.hasSelectedLine ? <LineForm /> : null;

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

        <div className={styles.form}>{formComponent}</div>
      </div>
    </div>
  );
}

function mapStateToProps(state) {
  return {
    hasSelectedLine: hasSelectedLine(state)
  };
}

export default connect(mapStateToProps)(Editor);
