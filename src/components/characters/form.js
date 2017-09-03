// @flow
import typeof { createCharacter } from "state/characters/actions";

import React, { Component } from "react";
import styles from "./styles.scss";

type Props = {
  createCharacter: createCharacter
};

export default class Form extends Component {
  state: { name: string };
  props: Props;

  constructor(props: Props) {
    super(props);
    this.state = { name: "" };
  }

  handleChange(event: SyntheticInputEvent) {
    this.setState({ name: event.target.value });
  }

  handleSubmit(event: SyntheticEvent) {
    event.preventDefault();
    this.props.createCharacter(this.state.name);
  }

  render() {
    return (
      <form className={styles.form} onSubmit={this.handleSubmit.bind(this)}>
        <input
          onChange={this.handleChange.bind(this)}
          type="text"
          value={this.state.name}
        />
        <button type="submit">Create Character</button>
      </form>
    );
  }
}
