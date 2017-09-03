// @flow
import React from "react";
import styles from "./styles.scss";

function Item({ character }: { character: Character }) {
  return <li className={styles.item}>{character.name}</li>;
}

export default function List({ characters }: { characters: Character[] }) {
  let items = characters.map(c => {
    return <Item key={c.id} character={c} />;
  });

  return <ul className={styles.list}>{items}</ul>;
}
