import React, { Component } from "react";
import { connect } from "react-redux";
import { createCharacter } from "state/characters/actions";
import { getSortedCharacters } from "state/characters/selectors";
import List from "./list";
import Form from "./form";
import styles from "./styles.scss";
import { infoBox } from "shared_styles/info_box.scss";

class Characters extends Component {
  constructor(props) {
    super(props);

    this.state = { isOpen: false };
    this._hideDropdown = this._hideDropdown.bind(this);
    this._showDropdown = this._showDropdown.bind(this);
  }

  componentDidMount() {
    window.addEventListener("click", this._hideDropdown);
  }

  componentWillUnmount() {
    window.removeEventListener("click", this._hideDropdown);
  }

  _hideDropdown(e) {
    const { isOpen } = this.state;
    if (isOpen) {
      this.setState({ isOpen: false });
    }
  }

  _showDropdown(e) {
    e.stopPropagation();
    const { isOpen } = this.state;
    if (!isOpen) {
      this.setState({ isOpen: true });
    }
  }

  render() {
    const { isOpen } = this.state;
    return (
      <div className={styles.wrapper}>
        <a onClick={this._showDropdown}>Characters</a>
        {isOpen ? this.getMenu() : null}
      </div>
    );
  }

  getMenu() {
    const { createCharacter, characters } = this.props;
    return (
      <div
        className={[infoBox, styles.popover].join(" ")}
        onClick={e => e.stopPropagation()}
      >
        <List characters={characters} />
        <Form createCharacter={createCharacter} />
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    characters: getSortedCharacters(state)
  };
}

export default connect(mapStateToProps, { createCharacter })(Characters);
