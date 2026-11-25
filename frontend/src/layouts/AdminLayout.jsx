import { useState } from 'react';
import { Boxes, ClipboardList, LayoutDashboard, LogOut, Menu, Store, Tags, Users, X } from 'lucide-react';
import { Link } from 'react-router-dom';

const navigation = [
  ['overview', 'Overview', LayoutDashboard],
  ['shops', 'Shops', Store],
  ['categories', 'Categories', Tags],
  ['products', 'Products', Boxes],
  ['orders', 'Orders', ClipboardList],
  ['users', 'Users', Users],
];

export default function AdminLayout({ children, activeSection, onNavigate, user, onLogout }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  function choose(section) {
    onNavigate(section);
    setMobileOpen(false);
  }

  return <div className="admin-shell">
    <aside className={`admin-sidebar${mobileOpen ? ' is-open' : ''}`} aria-label="Admin navigation">
      <Link to="/" className="admin-brand" aria-label="Velnora home"><img src="/velnora-logo-reversed.svg" alt="Velnora" /></Link>
      <p className="admin-sidebar-label">Administration</p>
      <nav className="admin-nav">
        {navigation.map(([id, label, Icon]) => <button key={id} type="button" className={activeSection === id ? 'is-active' : ''} aria-current={activeSection === id ? 'page' : undefined} onClick={() => choose(id)}><Icon size={18} /><span>{label}</span></button>)}
      </nav>
      <div className="admin-sidebar-bottom">
        <div className="admin-user"><span className="admin-user-avatar">{user?.name?.charAt(0) || 'V'}</span><span><b>{user?.name || 'Velnora Admin'}</b><small>Administrator</small></span></div>
        <button type="button" className="admin-logout" onClick={onLogout}><LogOut size={17} />Log out</button>
      </div>
    </aside>
    {mobileOpen && <button className="admin-scrim" type="button" aria-label="Close navigation" onClick={() => setMobileOpen(false)} />}
    <div className="admin-main">
      <header className="admin-mobile-bar">
        <button type="button" className="admin-menu-toggle" aria-label={mobileOpen ? 'Close navigation' : 'Open navigation'} aria-expanded={mobileOpen} onClick={() => setMobileOpen((open) => !open)}>{mobileOpen ? <X size={21} /> : <Menu size={21} />}</button>
        <img src="/velnora-logo.svg" alt="Velnora" />
        <span>Admin</span>
      </header>
      <main className="admin-content">{children}</main>
    </div>
  </div>;
}
