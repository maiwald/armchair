// @flow
import { clone } from "lodash";

function getCharacters(state: State): CharacterState {
  return state.characters;
}

export function getSortedCharacters(state: State): Character[] {
  return clone(getCharacters(state)).sort((lhs, rhs) =>
    lhs.name.localeCompare(rhs.name)
  );
}

export function getCharacter(state: State, id: number): ?Character {
  return getCharacters(state).find(c => c.id == id);
}

export function getCharacterNames(state: State): string[] {
  return state.characters.map(c => c.name);
}
