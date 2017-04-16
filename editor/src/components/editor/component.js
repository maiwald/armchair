import React from 'react';
import Characters from 'components/characters/component';
import Dialogue from 'components/dialogue/component';
import LineForm from 'components/line_form/component';
import Notice from 'components/notice';
import styles from './styles.css';

export default function Editor() {
  return (
    <div>
      <Notice />
      <div className={styles.wrapper}>
        <div className={styles.sidebarLeft}>
          <Characters />
        </div>

        <div className={styles.main}>
          <Dialogue />
          <LineForm />
        </div>
      </div>
    </div>
  );
}