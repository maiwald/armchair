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

export default function OutboundLines({ lines }) {
  const lineComponents = lines.map(l => {
    return (
      <li>
        {l.get("text")}
      </li>
    );
  });

  return (
    <section>
      <label>
        {getTitle(lines)}
      </label>
      <ul>
        {lineComponents}
      </ul>
    </section>
  );
}
