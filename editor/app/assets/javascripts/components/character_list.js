import React from 'react';
import { connect } from 'react-redux';

function CharacterItem({ character }) {
  return <li>{character.get('name')}</li>;
}

function CharacterList({ characters }) {
  let items = characters.map(c => {
    return <CharacterItem key={c.get('id')} character={c} />;
  });

  return <ul>{items}</ul>;
}

function mapStateToProps(state) {
  return { characters: state.get('characters') };
}

export default connect(mapStateToProps)(CharacterList);
