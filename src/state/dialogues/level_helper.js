// @flow
import { isEmpty, without } from "lodash";

export default function getLevels(lines: Line[], connections: Connection[]) {
  const levels = resolveDependencies(lines.map(l => l.id), [], connections);

  return lines.reduce((memo, l) => {
    memo[l.id] = levels.findIndex(deps => {
      return deps.includes(l.id);
    });
    return memo;
  }, {});
}

function resolveDependencies(
  unresolvedIds: number[],
  resolvedIds: number[][],
  connections: Connection[]
) {
  if (isEmpty(unresolvedIds)) {
    return resolvedIds;
  } else {
    const resolvableIds = unresolvedIds.filter(lineId => {
      const dependencies = getLineDependencies(connections, lineId);
      return dependencies.every(dependencyId => {
        return resolvedIds.some(ids => ids.includes(dependencyId));
      });
    });

    if (isEmpty(resolvableIds)) {
      throw new Error(
        "cannot calculate line levels because of cyclic dependency!"
      );
    }

    return resolveDependencies(
      without(unresolvedIds, ...resolvableIds),
      resolvedIds.concat([resolvableIds]),
      connections
    );
  }
}

function getLineDependencies(
  connections: Connection[],
  lineId: number
): number[] {
  return connections.filter(c => c.to == lineId).map(c => c.from);
}
