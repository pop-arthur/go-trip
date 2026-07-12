import React from 'react';
import styles from './Select.module.css';

const Select = ({ className, options, ...props }) => (
  <select className={`${styles.select} ${className || ''}`} {...props}>
    {options.map(opt => (
      <option key={opt.value} value={opt.value}>
        {opt.label}
      </option>
    ))}
  </select>
);

export default Select;