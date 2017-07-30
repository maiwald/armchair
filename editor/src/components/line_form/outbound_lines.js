import React from "react";

function Line({ line }) {
  return (
    <li>
      {line.get("text")}
    </li>
  );
}

function getTitle(lines) {
  return "Outbound connections";
}

export default function OutboundLines({ lines, addOutboundLine }) {
  const lineComponents = lines.map(l => <Line key={l.get("id")} line={l} />);
  return (
    <section>
      <label>
        {getTitle(lines)}
      </label>
      <a onClick={e => addOutboundLine(e)}>add</a>
      <ul>
        {lineComponents}
      </ul>
    </section>
  );
}
