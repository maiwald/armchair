// @flow

export function getNotifications(state: ApplicationState): Notification[] {
  return state.ui.notifications;
}
