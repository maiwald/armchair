// @flow
import React, { Component } from "react";
import { connect } from "react-redux";
import { Network } from "vis";
import { deleteLine, hoverLine, selectLine } from "state/dialogues/actions";
import {
  getDialogueEdges,
  getDialogueNodes,
  getSelectedLineId,
  getHoveredLineId,
  isInSelectionMode
} from "state/dialogues/selectors";
import { isUndefined, compact, isEqual, isNull, mapValues } from "lodash";
import styles from "./styles.scss";
import VIS_NETWORK_OPTIONS from "./vis_network_options.json";

type ValueProps = {
  nodes: DialogueNode[],
  edges: DialogueEdge[],
  selectedNodeId: ?string,
  hoveredNodeId: ?string,
  isInSelectionMode: boolean
};

type DispatchProps = {
  deleteLine: (?number) => void,
  hoverLine: (?number) => void,
  selectLine: (?number) => void
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
    if (!isNull(selectedNodeId)) {
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
    const classNames = compact([
      styles.container,
      isInSelectionMode ? styles.inModal : null
    ]).join(" ");

    return (
      <div>
        {this.getNodeActions()}
        <div ref="container" className={classNames} />
      </div>
    );
  }

  createNetwork() {
    const { container } = this.refs;
    const { selectLine, hoverLine } = this.props;

    const network = new Network(container, {}, VIS_NETWORK_OPTIONS);

    network.on("click", ({ pointer: { DOM: coords } }) => {
      const node = network.getNodeAt(coords);
      selectLine(isUndefined(node) ? undefined : parseInt(node));
    });

    network.on("hoverNode", ({ node }) => hoverLine(parseInt(node)));
    network.on("blurNode", () => hoverLine(undefined));

    return network;
  }

  getNodeActions() {
    const { hoveredNodeId, deleteLine } = this.props;
    if (isNull(hoveredNodeId)) {
      return null;
    } else {
      return (
        <div
          className={styles.nodeActions}
          style={this.getNodeActionDimensions(hoveredNodeId)}
        >
          <a
            className={styles.nodeAction}
            onClick={() => deleteLine(parseInt(hoveredNodeId))}
            title="Delete node"
          >
            <i className="fa fa-trash" />
          </a>
          <a
            className={styles.nodeAction}
            title="Add connection to existing line"
          >
            <i className="fa fa-link" />
          </a>
          <a className={styles.nodeAction} title="Add new line">
            <i className="fa fa-plus" />
          </a>
        </div>
      );
    }
  }

  getNodeActionDimensions(nodeId) {
    const {
      top: nodeTop,
      left: nodeLeft,
      right: nodeRight,
      bottom: nodeBottom
    } = this.network.getBoundingBox(nodeId);

    const { x: domLeft, y: domTop } = this.network.canvasToDOM({
      x: nodeLeft,
      y: nodeTop
    });

    const { x: domRight, y: domBottom } = this.network.canvasToDOM({
      x: nodeRight,
      y: nodeBottom
    });

    const domHeight = domBottom - domTop;
    const domWidth = domRight - domLeft;

    return mapValues(
      {
        left: domLeft,
        top: domTop + domHeight * 0.8,
        height: domHeight * 0.2 + 20,
        width: domWidth
      },
      Math.round
    );
  }
}

function mapStateToProps(state: any): ValueProps {
  const selectedNodeId = getSelectedLineId(state);
  const hoveredNodeId = getHoveredLineId(state);

  return {
    nodes: getDialogueNodes(state),
    edges: getDialogueEdges(state),
    selectedNodeId:
      typeof selectedNodeId == "number" ? selectedNodeId.toString() : null,
    hoveredNodeId:
      typeof hoveredNodeId == "number" ? hoveredNodeId.toString() : null,
    isInSelectionMode: isInSelectionMode(state)
  };
}

export default connect(mapStateToProps, { deleteLine, selectLine, hoverLine })(
  Dialogue
);
