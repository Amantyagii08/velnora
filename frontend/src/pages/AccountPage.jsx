import { useState } from 'react';
import { ArrowLeft, ArrowRight, LockKeyhole, UserRound } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, Card, Container, Input } from '../components/ui.jsx';
import Navbar from '../components/homepage/Navbar.jsx';
import Footer from '../components/homepage/Footer.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';

export default function AccountPage() {
  const { user, logout, updateProfile, changePassword } = useAuth();
  const cart = useCart();
  const navigate = useNavigate();
  const [profile, setProfile] = useState({ name: user?.name || '', phone: user?.phone || '' });
  const [password, setPassword] = useState({ current: '', next: '' });
  const [profileMessage, setProfileMessage] = useState('');
  const [passwordMessage, setPasswordMessage] = useState('');
  const [busy, setBusy] = useState(false);

  async function saveProfile(event) {
    event.preventDefault(); setProfileMessage('');
    if (!profile.name.trim()) { setProfileMessage('Enter your name to continue.'); return; }
    if (profile.phone && !/^\d{10,15}$/.test(profile.phone.trim())) { setProfileMessage('Enter a phone number with 10 to 15 digits.'); return; }
    setBusy(true);
    const result = await updateProfile(profile);
    setProfileMessage(result.ok ? 'Your profile has been updated.' : result.error);
    setBusy(false);
  }

  async function savePassword(event) {
    event.preventDefault(); setPasswordMessage('');
    if (!password.current || password.next.length < 8 || password.next.length > 72) { setPasswordMessage('Enter your current password and a new password between 8 and 72 characters.'); return; }
    setBusy(true);
    const result = await changePassword(password.current, password.next);
    setBusy(false);
    if (result.ok) { await logout(); navigate('/login', { replace: true, state: { message: 'Password updated. Sign in with your new password.' } }); return; }
    setPasswordMessage(result.error);
  }

  return <div className="commerce-page"><Navbar cart={cart.items.map(({ product, quantity }) => ({ product, quantity }))} cartCount={cart.count} onQuantityChange={() => {}} onOpenClearance={() => navigate('/clearance-deals')} user={user} onLogout={async () => { await logout(); navigate('/'); }} /><main className="commerce-main"><Container><Link className="commerce-back" to={user?.role === 'shopkeeper' ? '/shopkeeper' : '/'}><ArrowLeft size={16}/> Back to Velnora</Link><div className="commerce-heading"><p className="eyebrow">Account settings</p><h1>Your profile</h1><p>Manage the details connected to your Velnora account.</p></div><div className="account-settings-grid"><Card className="account-settings-card"><div className="checkout-section-head"><span className="checkout-icon"><UserRound size={19}/></span><div><h2>Personal details</h2><p>Update your name and contact number.</p></div></div><form onSubmit={saveProfile} className="account-settings-form"><Input label="Full name" value={profile.name} onChange={(event) => setProfile((value) => ({ ...value, name: event.target.value }))}/><Input label="Email address" value={user?.email || ''} disabled/><Input label="Phone number" value={profile.phone} onChange={(event) => setProfile((value) => ({ ...value, phone: event.target.value }))} inputMode="tel"/><Button type="submit" disabled={busy}>Save profile<ArrowRight size={16}/></Button>{profileMessage && <p className="checkout-message" role="status">{profileMessage}</p>}</form></Card><Card className="account-settings-card"><div className="checkout-section-head"><span className="checkout-icon"><LockKeyhole size={19}/></span><div><h2>Change password</h2><p>Your new password must contain at least 8 characters.</p></div></div><form onSubmit={savePassword} className="account-settings-form"><Input label="Current password" type="password" autoComplete="current-password" value={password.current} onChange={(event) => setPassword((value) => ({ ...value, current: event.target.value }))}/><Input label="New password" type="password" autoComplete="new-password" value={password.next} onChange={(event) => setPassword((value) => ({ ...value, next: event.target.value }))}/><Button type="submit" variant="secondary" disabled={busy}>Update password<ArrowRight size={16}/></Button>{passwordMessage && <p className="checkout-message" role="alert">{passwordMessage}</p>}</form></Card></div></Container></main><Footer/></div>;
}
