import React from 'react';
import { connect } from 'react-redux';
import { createCharacter } from 'state/characters/actions';
import { getSortedCharacters } from 'state/characters/selectors';
import List from './list';
import Form from './form';
import styles from './styles.scss';

function Characters({ characters, createCharacter }) {
  return (
    <div className={styles.characters}>
      <h2>Characters</h2>
      <List characters={characters} />
      <Form createCharacter={createCharacter} />
    </div>
  );
}

function mapStateToProps(state) {
  return {
    characters: getSortedCharacters(state)
  };
}

export default connect(mapStateToProps, { createCharacter })(Characters);