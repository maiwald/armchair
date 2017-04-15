import { trim } from 'lodash';
import { showTimedNotice } from 'state/notifications/actions';
import { getCharacterNames } from 'state/characters/selectors';

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
        fn: state => !getCharacterNames(state).includes(sanitizedName),
        msg: `Character '${sanitizedName}' already exists!`
      }
    ]
  };
}
