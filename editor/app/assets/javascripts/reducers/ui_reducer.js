import { Map } from 'immutable';

export const initialState = new Map({
  notice: null
});

export default function reducer(state, action) {
  switch (action.type) {
    case 'SHOW_NOTICE':
      return state.set('notice', action.notice);

    case 'HIDE_NOTICE':
      return state.set('notice', null);

    default:
      return state;
  }
}
