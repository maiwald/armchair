import React from 'react';
import { connect } from 'react-redux';

function Line({content, onClickDelete, onClickAdd}) {
  return (
    <div className="line">
      <acticle>{content}</acticle>
      <ul className="actions">
        <li><a onClick={() => onClickAdd}>add</a></li>
        <li><a onClick={() => onClickDelete}>delete</a></li>
      </ul>
    </div>
  );
}

export default Line;
