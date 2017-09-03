declare type Action = {
  type: "string",
  payload: any
};

declare type ActionThunk = (
  dispatch: (Action) => void,
  getState?: () => State
) => void;
