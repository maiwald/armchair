import { Map } from 'immutable';

export const initialState = new Map({
  notice: null,
  selectedLineId: null
});

export default function reducer(state, action) {
  switch (action.type) {
    case 'SHOW_NOTICE':
      return state.set('notice', action.notice);

    case 'HIDE_NOTICE':
      return state.set('notice', null);

    case 'SHOW_LINE_FORM':
      return state.set('selectedLineId', action.lineId);

    case 'HIDE_LINE_FORM':
      return state.set('selectedLineId', null);

    default:
      return state;
  }
}
