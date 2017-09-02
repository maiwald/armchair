import React, { Component } from "react";
import styles from "./styles.scss";

export default class Form extends Component {
  constructor(props) {
    super(props);
    this.state = { name: "" };

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleChange(event) {
    this.setState({ name: event.target.value });
  }

  handleSubmit(event) {
    event.preventDefault();
    this.props.createCharacter(this.state.name);
  }

  render() {
    return (
      <form className={styles.form} onSubmit={this.handleSubmit}>
        <input
          onChange={this.handleChange}
          type="text"
          value={this.state.name}
        />
        <button type="submit">Create Character</button>
      </form>
    );
  }
}
