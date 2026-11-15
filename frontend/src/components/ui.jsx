import { forwardRef } from 'react';

export function Container({ as: Element = 'div', className = '', children, ...props }) {
  return <Element className={`ui-container ${className}`} {...props}>{children}</Element>;
}

export function Button({ variant = 'primary', size = 'md', className = '', type = 'button', children, ...props }) {
  return <button type={type} className={`ui-button ui-button--${variant} ui-button--${size} ${className}`} {...props}>{children}</button>;
}

export function Card({ className = '', children, ...props }) {
  return <div className={`ui-card ${className}`} {...props}>{children}</div>;
}

export const Input = forwardRef(function Input({ label, hint, error, id, className = '', ...props }, ref) {
  const inputId = id || (label ? label.toLowerCase().replace(/[^a-z0-9]+/g, '-') : undefined);
  return (
    <div className={`ui-field ${className}`}>
      {label && <label className="ui-label" htmlFor={inputId}>{label}</label>}
      <input ref={ref} id={inputId} className={`ui-input${error ? ' is-invalid' : ''}`} aria-invalid={Boolean(error)} {...props} />
      {(error || hint) && <span className={`ui-hint${error ? ' ui-hint--error' : ''}`}>{error || hint}</span>}
    </div>
  );
});

export function Badge({ tone = 'neutral', className = '', children, ...props }) {
  return <span className={`ui-badge ui-badge--${tone} ${className}`} {...props}>{children}</span>;
}

export function SectionHeading({ eyebrow, title, description, className = '' }) {
  return (
    <div className={`ui-section-heading ${className}`}>
      {eyebrow && <p className="ui-eyebrow">{eyebrow}</p>}
      <h2>{title}</h2>
      {description && <p className="ui-section-description">{description}</p>}
    </div>
  );
}
