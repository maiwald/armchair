// @flow
import { isEmpty, trim } from "lodash";
import { showTimedNotice } from "state/notifications/actions";
import { getCharacterNames } from "state/characters/selectors";
import { RESET_CHARACTERS, CREATE_CHARACTER } from "state/action_types";

export function resetCharacters(characters: Character[]) {
  return {
    type: RESET_CHARACTERS,
    payload: {
      characters
    }
  };
}

export function createCharacter(name: string) {
  const sanitizedName = trim(name);

  return {
    type: CREATE_CHARACTER,
    payload: {
      name: sanitizedName
    },
    validations: [
      {
        fn: (state: State) => !getCharacterNames(state).includes(sanitizedName),
        msg: `Character '${sanitizedName}' already exists!`
      },
      {
        fn: () => !isEmpty(sanitizedName),
        msg: "Character name must not be empty!"
      }
    ]
  };
}
