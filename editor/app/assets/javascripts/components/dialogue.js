import React, { Component } from 'react';
import { connect } from 'react-redux';
import { Network } from 'vis';
import { List, Map } from 'immutable';
import { showLineForm, hideLineForm } from 'actions/line_actions.js';
import { isNull, first, size } from 'lodash';

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
    fixed: true
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
    this.handleClick = this.handleClick.bind(this);
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
    return <div style={{ height: '80vh' }} ref="container" />;
  }

  createNetwork() {
    const { container } = this.refs;
    const { selectedLineId } = this.props;

    const network = new Network(
      container,
      {
        nodes: this.getNodes(),
        edges: this.getEdges()
      },
      VIS_NETWORK_OPTIONS
    );

    if (!isNull(selectedLineId)) {
      network.selectNodes([selectedLineId]);
    }

    network.on('click', this.handleClick);

    return network;
  }

  handleClick(params) {
    const { nodes } = params;
    if (size(nodes) == 1) {
      this.props.showLineForm(first(nodes));
    } else {
      this.props.hideLineForm();
    }
  }

  getNodes() {
    const levels = this.getLevels();

    return this.props.lines
      .map(l => {
        return {
          id: l.get('id'),
          label: l.get('text'),
          group: l.get('character', 0),
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
  const dialogue = state.get('dialogue');

  return {
    lines: dialogue.get('lines'),
    connections: dialogue.get('connections'),
    selectedLineId: dialogue.get('selectedLineId')
  };
}

export default connect(mapStateToProps, { showLineForm, hideLineForm })(
  Dialogue
);
