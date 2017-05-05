import { fromJS } from "immutable";

const initialState = fromJS({
  notifications: []
});

export default function reducer(state = initialState, { type, payload }) {
  switch (type) {
    case "PUSH_NOTICE":
      return state.update("notifications", ns =>
        ns.unshift(fromJS({ id: Date.now(), text: payload.notice }))
      );

    case "POP_NOTICE":
      return state.update("notifications", ns => ns.pop());

    default:
      return state;
  }
}
