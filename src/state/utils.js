// @flow

import { max } from "lodash";

export function getNextId<T: { id: number }>(list: Array<T>): number {
  return max(list.map(n => n.id)) + 1;
}
