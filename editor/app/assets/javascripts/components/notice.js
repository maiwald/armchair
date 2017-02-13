import React from 'react';
import { connect } from 'react-redux';
import { isNull } from 'lodash';

const Notice = ({notice}) => {
  return isNull(notice) ? null : <div>{notice}</div>;
}

function mapStateToProps(state) {
  return {
    notice: state.getIn(['ui', 'notice'])
  };
}

export default connect(mapStateToProps)(Notice);
