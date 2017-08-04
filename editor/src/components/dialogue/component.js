// @flow
import React, { Component } from "react";
import { connect } from "react-redux";
import { Network } from "vis";
import { selectLine } from "state/dialogues/actions";
import {
  getSelectedLineId,
  isInSelectionMode,
  getDialogue
} from "state/dialogues/selectors";
import { isEqual, round, mapValues, isNull, isUndefined } from "lodash";
import getLevels from "./level_helper";
import styles from "./styles.css";
import VIS_NETWORK_OPTIONS from "./vis_network_options.json";

type ValueProps = {
  nodes: DialogueNode[],
  edges: DialogueEdge[],
  selectedNodeId: ?string,
  isInSelectionMode: boolean
};

type DispatchProps = {
  selectLine: (nodeId: string) => void
};

type Props = ValueProps & DispatchProps;

class Dialogue extends Component {
  network: any;
  props: Props;

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
      !isEqual(this.props.nodes, prevProps.nodes) ||
      !isEqual(this.props.edges, prevProps.edges)
    ) {
      this.setData();
      this.setSelectedNode();
    }

    if (this.props.selectedNodeId != prevProps.selectedNodeId) {
      this.setSelectedNode();
    }
  }

  setSelectedNode() {
    const { selectedNodeId } = this.props;
    if (!isUndefined(selectedNodeId)) {
      this.network.selectNodes([selectedNodeId], null);
    } else {
      this.network.unselectAll();
    }
  }

  setData() {
    const { nodes, edges } = this.props;
    this.network.setData({ nodes, edges });
  }

  render() {
    const { isInSelectionMode } = this.props;
    return (
      <div
        ref="container"
        style={{ height: "100vh" }}
        className={isInSelectionMode ? styles.inModal : null}
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
}

function mapStateToProps(state): ValueProps {
  const dialogue = getDialogue(state);

  const lines = dialogue.get("lines");
  const connections = dialogue.get("connections");

  const levels = getLevels(lines, connections);

  const nodes: DialogueNode[] = lines
    .map(l => {
      return {
        id: l.get("id"),
        label: l.get("text"),
        group: l.get("characterId"),
        level: levels.get(l.get("id"))
      };
    })
    .toJS();

  const edges: DialogueEdge[] = connections
    .map(c => {
      return {
        from: c.get("from"),
        to: c.get("to")
      };
    })
    .toJS();

  const selectedNodeId: ?number = getSelectedLineId(state);

  return {
    nodes,
    edges,
    selectedNodeId:
      typeof selectedNodeId == "number" ? selectedNodeId.toString() : null,
    isInSelectionMode: isInSelectionMode(state)
  };
}

export default connect(mapStateToProps, {
  selectLine
})(Dialogue);
