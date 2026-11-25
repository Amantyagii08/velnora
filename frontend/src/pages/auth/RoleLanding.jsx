import { Link } from 'react-router-dom';
import { LogOut, Store, ShieldCheck } from 'lucide-react';
import { Button } from '../../components/ui.jsx';
import './auth.css';

export default function RoleLanding({ user, onLogout }) {
  const shopkeeper = user.role === 'shopkeeper';
  const Icon = shopkeeper ? Store : ShieldCheck;
  return <main className="role-landing"><Link to="/" className="role-brand"><img src="/velnora-logo.svg" alt="Velnora" /></Link><section className="role-card"><span className="role-icon"><Icon size={26} /></span><p className="ui-eyebrow">{shopkeeper ? 'Shopkeeper access' : 'Administrator access'}</p><h1>Welcome, {user.name}.</h1><p>You’re signed in with the {shopkeeper ? 'shopkeeper' : 'admin'} demo role. That workspace will be added in a later task.</p><Button variant="secondary" onClick={onLogout}><LogOut size={16} /> Log out</Button></section></main>;
}
