function getCharacters(state) {
  return state.get('characters');
}

export function getSortedCharacters(state) {
  return getCharacters(state).sortBy(
    c => c.get('name'),
    (lhs, rhs) => lhs.localeCompare(rhs)
  );
}

export function getCharacter(state, id) {
  return getCharacters(state).find(c => c.get('id') == id);
}

export function getCharacterNames(state) {
  return state.get('characters').map(c => c.get('name'));
}
