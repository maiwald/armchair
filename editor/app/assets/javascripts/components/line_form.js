import React, { Component } from 'react';
import { connect } from 'react-redux';
import { updateLine } from '../actions/line_actions';
import { isUndefined } from 'lodash'

class LineForm extends Component {
  constructor(props) {
    super(props);

    const { line } = this.props;
    this.state = { text: line.get('text') };

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentWillReceiveProps({ line }) {
    this.setState({ text: line.get('text') });
  }

  render() {
    return (
      <form onSubmit={this.handleSubmit}>
        <input type='text' onChange={this.handleChange} value={this.state.text} />
        <button type='submit'>Save</button>
      </form>
    );
  }

  handleChange(event) {
    this.setState({ text: event.target.value });
  }

  handleSubmit(event) {
    event.preventDefault();
    this.props.updateLine(this.props.line.get('id'), this.state.text);
  }
}

function FormWrapper({ line, updateLine }) {
  if (isUndefined(line)) {
    return null;
  } else {
    return <LineForm
      line={line}
      updateLine={updateLine}
    />;
  }
}

function mapStateToProps(state) {
  const selectedLineId = state.getIn(['ui', 'selectedLineId']);

  const line = state
    .getIn(['data', 'dialogue', 'lines'])
    .find(l => { return l.get('id') == selectedLineId });

  return {
    line
  };
}

export default connect(mapStateToProps, {updateLine})(FormWrapper);
