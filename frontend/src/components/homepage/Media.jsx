import { Image as ImageIcon } from 'lucide-react';
import { useState } from 'react';

export function Media({ src, alt, className = '', fallbackLabel = 'Image unavailable' }) {
  const [failed, setFailed] = useState(false);
  return (
    <div className={`media-frame ${className}`}>
      {!failed ? <img src={src} alt={alt} loading="lazy" onError={() => setFailed(true)} /> : (
        <div className="media-fallback" role="img" aria-label={fallbackLabel}><ImageIcon size={24} strokeWidth={1.6} /><span>{fallbackLabel}</span></div>
      )}
    </div>
  );
}
