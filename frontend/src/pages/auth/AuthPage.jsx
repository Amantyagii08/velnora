import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Eye, EyeOff, ShieldCheck } from 'lucide-react';
import { Button, Input } from '../../components/ui.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import './auth.css';

function AuthLayout({ eyebrow, title, description, children, mode }) {
  return <main className="auth-page"><section className={`auth-shell auth-shell--${mode}`}>
    <aside className="auth-brand-panel"><div className="auth-brand-frame" aria-hidden="true" /><Link className="auth-brand-logo" to="/" aria-label="Velnora homepage"><img src="/velnora-logo-reversed.svg" alt="Velnora" /></Link><div className="auth-promise"><span><ShieldCheck size={18} /></span><p>Thoughtful finds from independent businesses.</p></div></aside>
    <div className="auth-form-panel"><header className="auth-topbar"><Link to="/" className="auth-back"><ArrowLeft size={16} /> Back to Velnora</Link><Link className="auth-mobile-logo" to="/" aria-label="Velnora homepage"><img src="/velnora-logo.svg" alt="Velnora" /></Link><span className="auth-top-eyebrow">{eyebrow}</span></header><div className="auth-form-wrap"><h1>{title}</h1><p className="auth-description">{description}</p>{children}</div><footer className="auth-footer"><p className="auth-footer-note">© {new Date().getFullYear()} Velnora</p><div><a href="#privacy" onClick={(event) => event.preventDefault()}>Privacy</a><a href="#terms" onClick={(event) => event.preventDefault()}>Terms</a></div></footer></div>
  </section></main>;
}

function PasswordInput({ label, value, onChange, error, autoComplete = 'current-password', id = 'password', action, placeholder = '' }) {
  const [visible, setVisible] = useState(false);
  return <div className="password-field"><Input id={id} label={label} type={visible ? 'text' : 'password'} value={value} onChange={onChange} error={error} autoComplete={autoComplete} placeholder={placeholder} />{action}<button type="button" className="password-toggle" aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible((current) => !current)}>{visible ? <EyeOff size={18} /> : <Eye size={18} />}</button></div>;
}

function validEmail(email) { return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email.trim()); }

export default function AuthPage({ mode }) {
  const navigate = useNavigate();
  const { login, register, isMock } = useAuth();
  const [values, setValues] = useState({ name: '', email: '', password: '', confirm: '', terms: false, role: 'customer' });
  const [errors, setErrors] = useState({});
  const [notice, setNotice] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const set = (key) => (event) => setValues((current) => ({ ...current, [key]: event.target.type === 'checkbox' ? event.target.checked : event.target.value }));
  const isRegister = mode === 'register';
  const isForgot = mode === 'forgot';
  async function submit(event) {
    event.preventDefault();
    setNotice('');
    const next = {};
    if (isRegister && !values.name.trim()) next.name = 'Please enter your name.';
    if (!values.email.trim()) next.email = 'Please enter your email address.';
    else if (!validEmail(values.email)) next.email = 'Enter a valid email address.';
    if (!isForgot && !values.password) next.password = 'Please enter your password.';
    else if (!isForgot && isRegister && values.password.length < 8) next.password = 'Use at least 8 characters.';
    if (isRegister && values.password && values.password !== values.confirm) next.confirm = 'Passwords do not match.';
    if (isRegister && !values.terms) next.terms = 'Please accept the terms to continue.';
    setErrors(next);
    if (Object.keys(next).length) return;
    if (isForgot) { setNotice('Password reset is not available yet. Please contact Velnora support for help.'); return; }
    setSubmitting(true);
    try {
      const result = isRegister ? await register(values) : await login(values.email, values.password);
      if (!result.ok) { setErrors({ form: result.error }); return; }
      navigate(result.user.role === 'customer' ? '/' : `/${result.user.role}`, { replace: true });
    } finally { setSubmitting(false); }
  }

  return <AuthLayout eyebrow={isRegister ? 'Join Velnora' : isForgot ? 'Account help' : 'Welcome back'} title={isRegister ? 'Create your account' : isForgot ? 'Reset your password' : 'Good to see you.'} description={isRegister ? 'A few details, then you can start finding good things close by.' : isForgot ? 'Password reset is not available yet. Contact Velnora support for help.' : 'Sign in to pick up where you left off.'} mode={mode}>
    <form className="auth-form" onSubmit={submit} noValidate>
      {isRegister && <><Input id="full-name" label="Full name" value={values.name} onChange={set('name')} error={errors.name} autoComplete="name" placeholder="Your name" /><fieldset className="auth-role-choice"><legend>Account type</legend><label><input type="radio" name="role" value="customer" checked={values.role === 'customer'} onChange={set('role')} /> Customer</label><label><input type="radio" name="role" value="shopkeeper" checked={values.role === 'shopkeeper'} onChange={set('role')} /> Shopkeeper</label></fieldset></>}
      <Input id="email" label="Email address" type="email" value={values.email} onChange={set('email')} error={errors.email} autoComplete="email" placeholder="you@example.com" />
      {!isForgot && <PasswordInput id="password" label="Password" value={values.password} onChange={set('password')} error={errors.password} autoComplete={isRegister ? 'new-password' : 'current-password'} placeholder={isRegister ? 'At least 8 characters' : 'Password'} action={!isRegister && <Link to="/forgot-password" className="auth-password-action">Forgot password?</Link>} />}
      {isRegister && <>
        <PasswordInput id="confirm-password" label="Confirm password" value={values.confirm} onChange={set('confirm')} error={errors.confirm} autoComplete="new-password" />
        <label className={`auth-terms${errors.terms ? ' has-error' : ''}`}><input type="checkbox" checked={values.terms} onChange={set('terms')} /><span>I agree to Velnora’s <a href="#terms" onClick={(event) => event.preventDefault()}>Terms</a> and <a href="#privacy" onClick={(event) => event.preventDefault()}>Privacy Policy</a>.</span></label>{errors.terms && <p className="auth-error">{errors.terms}</p>}
      </>}
      {errors.form && <p className="auth-form-error" role="alert">{errors.form}</p>}
      {notice && <p className="auth-success" role="status">{notice}</p>}
      <Button type="submit" className="auth-submit" size="lg" disabled={submitting}>{submitting ? 'Please wait…' : isRegister ? 'Create account' : isForgot ? 'Get account help' : 'Sign in'} <ArrowRight size={17} className="auth-submit-arrow" /></Button>
    </form>
    <p className="auth-switch">{isRegister ? <>Already have an account? <Link to="/login">Sign in</Link></> : isForgot ? <>Remembered it? <Link to="/login">Back to sign in</Link></> : <>New to Velnora? <Link to="/register">Create an account</Link></>}</p>
    {isMock && !isRegister && !isForgot && <p className="auth-demo-note">Demo mode is enabled for this development session.</p>}
  </AuthLayout>;
}
