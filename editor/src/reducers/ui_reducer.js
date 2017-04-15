import { fromJS } from 'immutable';

const initialState = fromJS({
  notice: null
});

export function selectNotice(state) {
  return state.getIn(['ui', 'notice']);
}

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
