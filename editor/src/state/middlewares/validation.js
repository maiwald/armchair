import { each, isEmpty, reject } from 'lodash';
import { showTimedNotice } from 'state/notifications/actions';

export default ({ dispatch, getState }) =>
  next =>
    action => {
      const failures = reject(action.validations, ({ fn }) => fn(getState()));

      if (isEmpty(failures)) {
        return next(action);
      } else {
        each(failures, ({ msg }) => dispatch(showTimedNotice(msg)));
      }
    };
