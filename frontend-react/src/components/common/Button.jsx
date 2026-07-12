import React from 'react';

const Button = ({ children, variant = 'primary', fullWidth, className, ...props }) => {
  const classes = `btn btn-${variant} ${fullWidth ? 'btn-full' : ''} ${className || ''}`;
  return (
    <button className={classes} {...props}>
      {children}
    </button>
  );
};

export default Button;