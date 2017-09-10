// @flow
import React from "react";
import { connect } from "react-redux";
import { getNotifications } from "state/ui/selectors";
import { isEmpty } from "lodash";

function Notifications({ notifications }: { notifications: Notification[] }) {
  if (isEmpty(notifications)) {
    return null;
  } else {
    const items = notifications.map(n => {
      return <li key={n.id}>{n.text}</li>;
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
