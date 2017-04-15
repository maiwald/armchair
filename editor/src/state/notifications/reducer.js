import { fromJS } from 'immutable';

const initialState = fromJS({
  notice: null
});

export default function reducer(state = initialState, { type, payload }) {
  switch (type) {
    case 'SHOW_NOTICE':
      return state.set('notice', payload.notice);

    case 'HIDE_NOTICE':
      return state.set('notice', null);

    default:
      return state;
  }
}
