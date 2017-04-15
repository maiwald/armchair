import { showTimedNotice } from './ui_actions';
import { trim } from 'lodash';
import { selectCharacterNames } from 'reducers/character_reducer';

export function resetCharacters(characters) {
  return {
    type: 'RESET_CHARACTERS',
    payload: {
      characters
    }
  };
}

export function createCharacter(name) {
  const sanitizedName = trim(name);

  return {
    type: 'CREATE_CHARACTER',
    payload: {
      name: sanitizedName
    },
    validations: [
      {
        fn: state => !selectCharacterNames(state).includes(sanitizedName),
        msg: `Character '${sanitizedName}' already exists!`
      }
    ]
  };
}
