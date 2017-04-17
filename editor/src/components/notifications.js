import React from 'react';
import { connect } from 'react-redux';
import { isEmpty } from 'lodash';
import { getNotifications } from 'state/notifications/selectors';

function Notifications({ notifications }) {
  if (isEmpty(notifications)) {
    return null;
  } else {
    const items = notifications.map(n => {
      return (
        <li key={n.get('id')}>
          {n.get('text')}
        </li>
      );
    });

    return <ul>{items}</ul>;
  }
}

function mapStateToProps(state) {
  return {
    notifications: getNotifications(state)
  };
}

export default connect(mapStateToProps)(Notifications);
