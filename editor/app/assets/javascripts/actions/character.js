import { showTimedNotice } from 'actions/ui';
import { getJSON, postJSON } from 'ajax_helpers';

export function resetCharacters(characters) {
  return {
    type: 'RESET_CHARACTERS',
    characters
  };
}

export function addCharacter(character) {
  return {
    type: 'ADD_CHARACTER',
    character
  };
}

export function loadCharacters() {
  return dispatch => {
    getJSON('/characters.json')
      .then(characters => dispatch(resetCharacters(characters)))
      .catch(e => dispatch(showTimedNotice(e.message)));
  };
}

export function createCharacter(name) {
  return dispatch => {
    postJSON('/characters.json', { name: name })
      .then(character => dispatch(addCharacter(character)))
      .catch(({error}) => dispatch(showTimedNotice(error)));
  };
}

