export function getCharacters(state) {
  return state
    .get('characters')
    .sortBy(c => c.get('name'), (lhs, rhs) => lhs.localeCompare(rhs));
}

export function getCharacterNames(state) {
  return state.get('characters').map(c => c.get('name'));
}
