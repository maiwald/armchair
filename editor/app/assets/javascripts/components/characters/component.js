import React from 'react';
import { connect } from 'react-redux';
import { createCharacter } from 'actions/character_actions';
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
    characters: state
      .get('characters')
      .sortBy(c => c.get('name'), (lhs, rhs) => lhs.localeCompare(rhs))
  };
}

export default connect(mapStateToProps, { createCharacter })(Characters);
