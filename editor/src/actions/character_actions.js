import { showTimedNotice } from './ui_actions';

export function resetCharacters(characters) {
  return {
    type: 'RESET_CHARACTERS',
    characters
  };
}

export function createCharacter(name) {
  return {
    type: 'CREATE_CHARACTER',
    name
  };
}
