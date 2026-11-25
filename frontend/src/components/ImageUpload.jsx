import { useEffect, useMemo, useState } from 'react';
import { ImagePlus, RotateCcw, Upload } from 'lucide-react';

const MAX_IMAGE_SIZE = 5 * 1024 * 1024;

export default function ImageUpload({ label, value, file, onChange, hint = 'JPG, PNG or WebP · up to 5 MB', compact = false }) {
  const [localError, setLocalError] = useState('');
  const preview = useMemo(() => file ? URL.createObjectURL(file) : value || '', [file, value]);
  useEffect(() => () => { if (file && preview.startsWith('blob:')) URL.revokeObjectURL(preview); }, [file, preview]);

  function chooseImage(event) {
    const selected = event.target.files?.[0] || null;
    event.target.value = '';
    if (selected && !selected.type.startsWith('image/')) { setLocalError('Choose an image file.'); return; }
    if (selected && selected.size > MAX_IMAGE_SIZE) { setLocalError('Image must be 5 MB or smaller.'); return; }
    setLocalError('');
    onChange(selected);
  }

  return <div className={`image-upload${compact ? ' image-upload--compact' : ''}`}>
    <div className="image-upload-preview" aria-label={preview ? `${label} preview` : `${label} placeholder`}>
      {preview ? <img src={preview} alt={`${label} preview`} /> : <div className="image-upload-empty"><ImagePlus size={25}/><span>Image preview</span></div>}
    </div>
    <div className="image-upload-controls">
      <div className="image-upload-copy"><b>{label}</b><span>{file ? file.name : value ? 'Current image' : 'Add an image to make this listing stand out.'}</span><small>{hint}</small></div>
      <label className="image-upload-button"><Upload size={16}/>{file ? 'Choose another' : value ? 'Replace image' : 'Choose image'}<input type="file" accept="image/jpeg,image/png,image/webp" onChange={chooseImage} /></label>
      {file && <button type="button" className="image-upload-reset" onClick={() => { setLocalError(''); onChange(null); }}><RotateCcw size={14}/>Undo selection</button>}
      {localError && <span className="image-upload-error" role="alert">{localError}</span>}
    </div>
  </div>;
}
