import React, { Component } from 'react';
import { connect } from 'react-redux';
import { Network } from 'vis';
import { List, Map } from 'immutable';
import {
  showLineForm,
  setLineFormPosition,
  hideLineForm
} from 'state/dialogues/actions';
import { getDialogue } from 'state/dialogues/selectors';
import { round, mapValues, isNull, first, size } from 'lodash';

const VIS_NETWORK_OPTIONS = {
  layout: {
    improvedLayout: true,
    hierarchical: {
      direction: 'LR',
      levelSeparation: 200,
      nodeSpacing: 150
    }
  },
  nodes: {
    shape: 'box',
    fixed: true,
    widthConstraint: 130
  },
  edges: {
    arrows: 'to'
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
    return <div style={{ height: '100vh' }} ref="container" />;
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

    network.on('click', ({ nodes }) => {
      if (size(nodes) == 1) {
        const node = first(nodes);
        this.props.showLineForm(node);
        this.props.setLineFormPosition(this.getFormPosition(node));
      } else {
        this.props.hideLineForm();
      }
    });

    network.on('dragging', () => {
      const nodes = network.getSelectedNodes();
      if (size(nodes) == 1) {
        const node = first(nodes);
        this.props.setLineFormPosition(this.getFormPosition(node));
      }
    });

    return network;
  }

  getFormPosition(nodeId) {
    const { top, left, right } = this.network.getBoundingBox(nodeId);
    const pLeft = this.network.canvasToDOM({ x: left, y: top });
    const pRight = this.network.canvasToDOM({ x: right, y: top });
    return mapValues(
      {
        x: pLeft.x + (pRight.x - pLeft.x) / 2,
        y: pLeft.y
      },
      round
    );
  }

  getNodes() {
    const levels = this.getLevels();

    return this.props.lines
      .map(l => {
        return {
          id: l.get('id'),
          label: l.get('text'),
          group: l.get('characterId', 0),
          level: levels.get(l.get('id'))
        };
      })
      .toJS();
  }

  getEdges() {
    return this.props.connections
      .map(c => {
        return {
          from: c.get('from'),
          to: c.get('to')
        };
      })
      .toJS();
  }

  getLevels() {
    const { lines } = this.props;
    let orderedDependencies = this.resolveDependencies(lines.toSet());

    return lines.reduce(
      (memo, l) => {
        return memo.set(
          l.get('id'),
          orderedDependencies.findIndex(deps => {
            return deps.contains(l);
          })
        );
      },
      new Map()
    );
  }

  resolveDependencies(unresolved, resolved = new List()) {
    if (unresolved.isEmpty()) {
      return resolved;
    } else {
      const resolvable = unresolved.filter(line => {
        return this.getLineDependencies(line).isSubset(resolved.flatten());
      });

      return this.resolveDependencies(
        unresolved.subtract(resolvable),
        resolved.push(resolvable)
      );
    }
  }

  getLineDependencies(line) {
    return this.props.connections
      .filter(c => c.get('to') == line.get('id'))
      .map(c => c.get('from'))
      .toSet();
  }
}

function mapStateToProps(state) {
  const dialogue = getDialogue(state);

  return {
    lines: dialogue.get('lines'),
    connections: dialogue.get('connections')
  };
}

export default connect(mapStateToProps, {
  setLineFormPosition,
  showLineForm,
  hideLineForm
})(Dialogue);
