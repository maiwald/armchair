import { List, Map } from "immutable";

export default function getLevels(lines, connections) {
  let levels = resolveDependencies(
    lines.map(l => l.get("id")).toSet(),
    undefined,
    connections
  );

  return lines.reduce((memo, l) => {
    return memo.set(
      l.get("id"),
      levels.findIndex(deps => {
        return deps.contains(l.get("id"));
      })
    );
  }, new Map());
}

function resolveDependencies(
  unresolvedIds,
  resolvedIds = new List(),
  connections
) {
  if (unresolvedIds.isEmpty()) {
    return resolvedIds;
  } else {
    const resolvableIds = unresolvedIds.filter(lineId => {
      return getLineDependencies(connections, lineId).isSubset(
        resolvedIds.flatten()
      );
    });

    return resolveDependencies(
      unresolvedIds.subtract(resolvableIds),
      resolvedIds.push(resolvableIds),
      connections
    );
  }
}

function getLineDependencies(connections, lineId) {
  return connections
    .filter(c => c.get("to") == lineId)
    .map(c => c.get("from"))
    .toSet();
}
