export function getNotifications(state) {
  return state.getIn(["ui", "notifications"]);
}
