import React, { Component } from 'react';
import { connect } from 'react-redux';
import { updateLine } from 'state/dialogues/actions';
import { isUndefined } from 'lodash';

const style = {};

class LineForm extends Component {
  constructor(props) {
    super(props);

    this.state = { line: props.line };

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentWillReceiveProps({ line }) {
    this.setState({ line: line });
  }

  render() {
    const { line } = this.state;

    return (
      <div className={style.lineForm}>
        <header>ID: {line.get('id')}</header>
        <section className={style.text}>
          <form className={style.foo} onSubmit={this.handleSubmit}>
            <input
              type="text"
              onChange={this.handleChange}
              value={line.get('text')}
            />
            <button type="submit">Save</button>
          </form>
        </section>
        <section className={style.connections}>
          <p>Inbound connections</p>
          <ul>
            <li>some inbound line <a>x</a></li>
            <li>some inbound line <a>x</a></li>
          </ul>
        </section>
      </div>
    );
  }

  handleChange(event) {
    const { line } = this.state;
    this.setState({ line: line.set('text', event.target.value) });
  }

  handleSubmit(event) {
    event.preventDefault();
    const { line } = this.state;
    this.props.updateLine(line.get('id'), line.get('text'));
  }
}

function FormWrapper({ line, updateLine }) {
  if (isUndefined(line)) {
    return null;
  } else {
    return <LineForm line={line} updateLine={updateLine} />;
  }
}

function mapStateToProps(state) {
  const dialogue = state.get('dialogue');
  const selectedLineId = dialogue.get('selectedLineId');

  const line = dialogue.get('lines').find(line => {
    return line.get('id') == selectedLineId;
  });

  return { line };
}

export default connect(mapStateToProps, { updateLine })(FormWrapper);
