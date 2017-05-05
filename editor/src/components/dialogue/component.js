import React, { Component } from "react";
import { connect } from "react-redux";
import { Network } from "vis";
import { showLineForm, hideLineForm } from "state/dialogues/actions";
import { getDialogue } from "state/dialogues/selectors";
import { round, mapValues, isNull, first, size } from "lodash";
import getLevels from "./level_helper";

const VIS_NETWORK_OPTIONS = {
  layout: {
    improvedLayout: true,
    hierarchical: {
      direction: "LR",
      levelSeparation: 200,
      nodeSpacing: 150
    }
  },
  nodes: {
    shape: "box",
    fixed: true,
    widthConstraint: 130
  },
  edges: {
    arrows: "to"
  },
  physics: {
    enabled: false
  }
};

class Dialogue extends Component {
  constructor(props) {
    super(props);

    this.network = null;
  }

  componentDidMount() {
    this.network = this.createNetwork();
  }

  componentWillUnmount() {
    this.network.destroy();
  }

  componentWillUpdate() {
    this.network.destroy();
  }

  componentDidUpdate() {
    this.network = this.createNetwork();
  }

  render() {
    return <div style={{ height: "100vh" }} ref="container" />;
  }

  createNetwork() {
    const { container } = this.refs;

    const network = new Network(
      container,
      {
        nodes: this.getNodes(),
        edges: this.getEdges()
      },
      VIS_NETWORK_OPTIONS
    );

    network.on("click", ({ nodes }) => {
      if (size(nodes) == 1) {
        const node = first(nodes);
        this.props.showLineForm(node);
      } else {
        this.props.hideLineForm();
      }
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
    connections: dialogue.get("connections")
  };
}

export default connect(mapStateToProps, {
  showLineForm,
  hideLineForm
})(Dialogue);
