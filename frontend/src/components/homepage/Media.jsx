import { useState } from 'react';

export function Media({ src, alt, className = '', fallbackLabel = 'Image unavailable', priority = false }) {
  const [failed, setFailed] = useState(false);
  const fallbackInitials = fallbackLabel.split(/\s+/).map((word) => word[0]).filter(Boolean).slice(0, 2).join('').toUpperCase();
  return (
    <div className={`media-frame ${className}`}>
      {src && !failed ? <img src={src} alt={alt} loading={priority ? 'eager' : 'lazy'} decoding="async" fetchPriority={priority ? 'low' : 'auto'} onError={() => setFailed(true)} /> : (
        <div className="media-fallback" role="img" aria-label={fallbackLabel}><span className="media-fallback-initials">{fallbackInitials}</span><span className="media-fallback-rule" /><span>{fallbackLabel}</span></div>
      )}
    </div>
  );
}
