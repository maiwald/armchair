import React from "react";
import styles from "./styles.scss";

function Item({ character }) {
  return <li className={styles.item}>{character.get("name")}</li>;
}

export default function List({ characters }) {
  let items = characters.map(c => {
    return <Item key={c.get("id")} character={c} />;
  });

  return <ul className={styles.list}>{items}</ul>;
}
