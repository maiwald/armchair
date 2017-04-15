import { fromJS } from 'immutable';

const initialState = fromJS({
  selectedLineId: null,
  lines: [
    { id: 1, character: 1, text: 'Hey, who are you?', level: 0 },
    { id: 2, text: 'I could ask you the same.', level: 1 },
    { id: 3, text: 'My name does not matter.', level: 3 },
    { id: 4, character: 1, text: 'I am Hugo. And you...?', level: 2 },
    { id: 5, text: 'I am Hugo as well.', level: 3 },
    { id: 6, text: 'None of your business!', level: 3 },
    { id: 7, character: 1, text: 'Fine, be a jerk.', level: 4 },
    { id: 8, character: 1, text: 'Nice to meet you!', level: 4 },
    { id: 9, character: 1, text: 'Ok, bye!', level: 5 }
  ],
  connections: [
    { id: 1, from: 1, to: 2 },
    { id: 2, from: 2, to: 4 },
    { id: 3, from: 4, to: 5 },
    { id: 4, from: 4, to: 6 },
    { id: 5, from: 6, to: 7 },
    { id: 6, from: 5, to: 8 },
    { id: 7, from: 8, to: 9 },
    { id: 8, from: 1, to: 3 },
    { id: 9, from: 3, to: 7 },
    { id: 10, from: 7, to: 9 }
  ]
});

export default function reducer(state = initialState, { type, payload }) {
  switch (type) {
    case 'UPDATE_LINE':
      const { lineId, text } = payload;
      const lineIndex = state.get('lines').findIndex(line => {
        return line.get('id') == lineId;
      });
      return state.setIn(['lines', lineIndex, 'text'], text);

    case 'SHOW_LINE_FORM':
      return state.set('selectedLineId', payload.lineId);

    case 'HIDE_LINE_FORM':
      return state.set('selectedLineId', null);

    default:
      return state;
  }
}
