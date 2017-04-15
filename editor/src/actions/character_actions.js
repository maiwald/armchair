import { showTimedNotice } from './ui_actions';

export function resetCharacters(characters) {
  return {
    type: 'RESET_CHARACTERS',
    payload: {
      characters
    }
  };
}

export function createCharacter(name) {
  return {
    type: 'CREATE_CHARACTER',
    payload: {
      name
    }
  };
}
