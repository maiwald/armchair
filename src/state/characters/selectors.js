// @flow
import { clone } from "lodash";

function getCharacters(state: ApplicationState): CharacterState {
  return state.characters;
}

export function getSortedCharacters(state: ApplicationState): Character[] {
  return clone(getCharacters(state)).sort((lhs, rhs) =>
    lhs.name.localeCompare(rhs.name)
  );
}

export function getCharacter(state: ApplicationState, id: number): ?Character {
  return getCharacters(state).find(c => c.id == id);
}

export function getCharacterNames(state: ApplicationState): string[] {
  return state.characters.map(c => c.name);
}
