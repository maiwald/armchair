import React from 'react';
import { connect } from 'react-redux';
import { getNotice } from 'reducers/ui_reducer';
import { isNull } from 'lodash';

function Notice({ notice }) {
  return isNull(notice) ? null : <div>{notice}</div>;
}

function mapStateToProps(state) {
  return {
    notice: getNotice(state)
  };
}

export default connect(mapStateToProps)(Notice);
