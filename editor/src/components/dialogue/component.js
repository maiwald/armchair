// @flow
import React, { Component } from "react";
import { connect } from "react-redux";
import { Network } from "vis";
import { selectLine } from "state/dialogues/actions";
import {
  getSelectedLineId,
  isModalSelection,
  getDialogue
} from "state/dialogues/selectors";
import { round, mapValues, isNull, isUndefined } from "lodash";
import getLevels from "./level_helper";
import styles from "./styles.css";

const VIS_NETWORK_OPTIONS = {
  layout: {
    improvedLayout: true,
    hierarchical: {
      direction: "LR",
      levelSeparation: 200,
      nodeSpacing: 150
    }
  },
  interaction: {
    selectConnectedEdges: false,
    selectable: false
  },
  nodes: {
    shape: "box",
    fixed: true,
    widthConstraint: 130
  },
  edges: {
    arrows: {
      to: {
        scaleFactor: 0.5
      }
    },
    color: "ccc"
  },
  physics: {
    enabled: false
  }
};

class Dialogue extends Component {
  network: any;

  constructor(props) {
    super(props);

    this.network = null;
  }

  componentDidMount() {
    this.network = this.createNetwork();

    this.setData();
    this.setSelectedNode();
  }

  componentWillUnmount() {
    this.network.destroy();
  }

  componentDidUpdate(prevProps) {
    if (
      !this.props.lines.equals(prevProps.lines) ||
      !this.props.connections.equals(prevProps.connections)
    ) {
      this.setData();
      this.setSelectedNode();
    }

    if (this.props.selectedLineId != prevProps.selectedLineId) {
      this.setSelectedNode();
    }
  }

  setSelectedNode() {
    const { selectedLineId } = this.props;
    if (!isUndefined(selectedLineId)) {
      this.network.selectNodes([selectedLineId], null);
    } else {
      this.network.unselectAll();
    }
  }

  setData() {
    this.network.setData({
      nodes: this.getNodes(),
      edges: this.getEdges()
    });
  }

  render() {
    const { isModalNodeSelection } = this.props;
    return (
      <div
        ref="container"
        style={{ height: "100vh" }}
        className={isModalNodeSelection ? styles.inModal : null}
      />
    );
  }

  createNetwork() {
    const { container } = this.refs;

    const network = new Network(container, {}, VIS_NETWORK_OPTIONS);

    network.on("click", ({ pointer: { DOM: coords } }) => {
      const node = network.getNodeAt(coords);
      this.props.selectLine(node);
    });

    return network;
  }

  getNodes() {
    const { lines, connections } = this.props;
    const levels = getLevels(lines, connections);

    return lines
      .map(l => {
        return {
          id: l.get("id"),
          label: l.get("text"),
          group: l.get("characterId", 0),
          level: levels.get(l.get("id"))
        };
      })
      .toJS();
  }

  getEdges() {
    return this.props.connections
      .map(c => {
        return {
          from: c.get("from"),
          to: c.get("to")
        };
      })
      .toJS();
  }
}

function mapStateToProps(state) {
  const dialogue = getDialogue(state);

  return {
    lines: dialogue.get("lines"),
    connections: dialogue.get("connections"),
    selectedLineId: getSelectedLineId(state),
    isModalNodeSelection: isModalSelection(state)
  };
}

export default connect(mapStateToProps, {
  selectLine
})(Dialogue);
