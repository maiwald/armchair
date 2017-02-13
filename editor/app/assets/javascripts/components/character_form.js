import React, { Component } from 'react';
import { connect } from 'react-redux';
import { createCharacter } from 'actions/character'

class CharacterForm extends Component {
  constructor(props) {
    super(props);
    this.state = { name: '' };

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleChange(event) {
    this.setState({name: event.target.value});
  }

  handleSubmit(event) {
    event.preventDefault();
    this.props.createCharacter(this.state.name);
  }

  render() {
    return (
      <form onSubmit={this.handleSubmit}>
        <label>
          <input onChange={this.handleChange} type='text' value={this.state.name} />
        </label>
        <button type='submit'>Create {this.props.resouceName}</button>
      </form>
    );
  }
}

export default connect(null, {createCharacter})(CharacterForm);
