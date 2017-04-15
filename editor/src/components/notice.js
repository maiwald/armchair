import React from 'react';
import { connect } from 'react-redux';
import { isNull } from 'lodash';
import { getNotice } from 'state/notifications/selectors';

function Notice({ notice }) {
  return isNull(notice) ? null : <div>{notice}</div>;
}

function mapStateToProps(state) {
  return {
    notice: getNotice(state)
  };
}

export default connect(mapStateToProps)(Notice);
