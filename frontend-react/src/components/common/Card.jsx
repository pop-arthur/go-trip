import React from 'react';

const Card = ({ children, title, icon }) => {
  return (
    <div className="card fade-in">
      {title && (
        <div className="card-title">
          {icon && <i className={`fas ${icon}`}></i>}
          {title}
        </div>
      )}
      {children}
    </div>
  );
};

export default Card;