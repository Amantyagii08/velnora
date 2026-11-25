import { useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, Search, X } from 'lucide-react';
import { Badge, Button, Card, Container, Input, SectionHeading } from './components/ui.jsx';
import Navbar from './components/homepage/Navbar.jsx';
import HeroSection from './components/homepage/HeroSection.jsx';
import CategoryCard from './components/homepage/CategoryCard.jsx';
import ProductCard from './components/homepage/ProductCard.jsx';
import ShopCard from './components/homepage/ShopCard.jsx';
import ClearanceCard from './components/homepage/ClearanceCard.jsx';
import ShopDetailView from './components/homepage/ShopDetailView.jsx';
import ClearanceDealsView from './components/homepage/ClearanceDealsView.jsx';
import ProductDetailView from './components/homepage/ProductDetailView.jsx';
import WhyVelnora from './components/homepage/WhyVelnora.jsx';
import Footer from './components/homepage/Footer.jsx';
import { useAuth } from './context/AuthContext.jsx';
import AuthPage from './pages/auth/AuthPage.jsx';
import CartPage from './pages/CartPage.jsx';
import CheckoutPage from './pages/CheckoutPage.jsx';
import OrderConfirmationPage from './pages/OrderConfirmationPage.jsx';
import OrderSummaryPage from './pages/OrderSummaryPage.jsx';
import AccountPage from './pages/AccountPage.jsx';
import OrdersPage from './pages/OrdersPage.jsx';
import ShopkeeperDashboard from './pages/shopkeeper/ShopkeeperDashboard.jsx';
import AdminDashboard from './pages/admin/AdminDashboard.jsx';
import { useCart } from './context/CartContext.jsx';
import { useMarketplaceData } from './context/MarketplaceDataContext.jsx';
import { apiConfigured } from './services/apiClient.js';
import { compareProducts } from './services/marketplaceApi.js';
import './App.css';

function MarketplaceHome() {
  const { user, logout } = useAuth();
  const { items: cart, count: cartCount, add: addToCart, setQuantity } = useCart();
  const { products, shops, categories, loading: marketplaceLoading, error: marketplaceError } = useMarketplaceData();
  const approvedShops = useMemo(() => shops.filter((shop) => (shop.status || 'Approved') === 'Approved'), [shops]);
  const approvedShopIds = useMemo(() => new Set(approvedShops.map((shop) => shop.id)), [approvedShops]);
  const publicProducts = useMemo(() => products.filter((product) => approvedShopIds.has(product.shopId)), [products, approvedShopIds]);
  const shopById = useMemo(() => new Map(shops.map((shop) => [shop.id, shop])), [shops]);
  const productById = useMemo(() => new Map(publicProducts.map((product) => [product.id, product])), [publicProducts]);
  const popularShops = useMemo(() => [...approvedShops].sort((a, b) => Number(b.rating) - Number(a.rating)), [approvedShops]);
  const navigate = useNavigate();
  const location = useLocation();
  const [productQuery, setProductQuery] = useState('');
  const [shopQuery, setShopQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');
  const [activeShop, setActiveShop] = useState('');
  const [activeProduct, setActiveProduct] = useState('');
  const [productOrigin, setProductOrigin] = useState('home');
  const [clearanceOpen, setClearanceOpen] = useState(false);
  const [comparisonResults, setComparisonResults] = useState(null);
  const [comparisonLoading, setComparisonLoading] = useState(false);
  const [comparisonError, setComparisonError] = useState('');

  useEffect(() => {
    const sections = document.querySelectorAll('.page-section, .why-section');
    if (!('IntersectionObserver' in window)) return;
    const observer = new IntersectionObserver((entries) => entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-revealed');
        observer.unobserve(entry.target);
      }
    }), { threshold: 0.01, rootMargin: '240px 0px' });
    sections.forEach((section) => {
      section.classList.add('scroll-reveal');
      observer.observe(section);
    });
    return () => observer.disconnect();
  }, []);

  useEffect(() => { setClearanceOpen(location.pathname === '/clearance-deals'); }, [location.pathname]);

  const filteredProducts = useMemo(() => {
    const query = productQuery.trim().toLowerCase();
    if (!query) return publicProducts;
    return publicProducts.filter((product) => [product.name, product.brand, product.variant, product.unit]
      .some((value) => value?.toLowerCase().includes(query)));
  }, [productQuery, publicProducts]);

  const filteredShops = useMemo(() => {
    const query = shopQuery.trim().toLowerCase();
    return popularShops.filter((shop) => {
      const matchesSearch = !query || `${shop.name} ${shop.category}`.toLowerCase().includes(query);
      const matchesCategory = Boolean(query) || activeCategory === 'all' || shop.categoryId === activeCategory;
      return matchesSearch && matchesCategory;
    });
  }, [shopQuery, activeCategory, popularShops]);

  const selectedShop = shopById.get(activeShop);
  const selectedShopProducts = selectedShop && (selectedShop.status || 'Approved') === 'Approved' ? publicProducts.filter((product) => product.shopId === selectedShop.id) : [];
  const clearanceProducts = publicProducts.filter((product) => product.clearance);
  function changeCartQuantity(productId, change) { const line = cart.find((item) => item.product.id === productId); if (line) setQuantity(line.key, line.quantity + change); }

  function chooseCategory(categoryId) {
    setActiveCategory((current) => current === categoryId ? 'all' : categoryId);
    setShopQuery('');
    document.getElementById('shops')?.scrollIntoView({ behavior: 'smooth' });
  }

  function openShop(shopId) {
    setActiveShop(shopId);
  }

  function openProduct(product, origin = 'home') {
    setActiveProduct(product.id);
    setProductOrigin(origin);
  }

  function selectComparedProduct(product) {
    if (productOrigin === 'shop') setActiveShop(product.shopId);
    setActiveProduct(product.id);
  }

  function resetProductSearch() {
    setProductQuery('');
  }

  function submitProductSearch(event) {
    event.preventDefault();
    document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' });
  }

  function openClearanceListing() {
    setActiveShop('');
    setActiveProduct('');
    setClearanceOpen(true);
    if (location.pathname !== '/clearance-deals') navigate('/clearance-deals');
  }

  function closeClearanceListing() {
    setClearanceOpen(false);
    if (location.pathname === '/clearance-deals') navigate('/', { replace: true });
  }

  const activeProductRecord = productById.get(activeProduct);
  useEffect(() => {
    if (!activeProductRecord || !apiConfigured || activeProductRecord.packSizeValue == null || activeProductRecord.packSizeUnit == null) {
      setComparisonResults(null); setComparisonLoading(false); setComparisonError(''); return undefined;
    }
    let alive = true; setComparisonLoading(true); setComparisonError('');
    compareProducts({
      brand: activeProductRecord.brand, name: activeProductRecord.name, variant: activeProductRecord.variant,
      packSizeValue: activeProductRecord.packSizeValue, packSizeUnit: activeProductRecord.packSizeUnit, page: 0, size: 100,
    }).then((response) => {
      if (!alive) return;
      const rows = Array.isArray(response) ? response : response?.content || [];
      const mapped = rows.map((row) => {
        const base = productById.get(String(row.productId));
        return base ? { ...base, price: Number(row.currentPrice), wasPrice: Number(row.originalPrice) > Number(row.currentPrice) ? Number(row.originalPrice) : null, available: row.available, stock: row.available ? base.stock : 0 } : null;
      }).filter(Boolean);
      setComparisonResults(mapped.length ? mapped : [activeProductRecord]);
    }).catch(() => {
      if (alive) { setComparisonResults([activeProductRecord]); setComparisonError('Prices from other shops could not be loaded right now.'); }
    }).finally(() => { if (alive) setComparisonLoading(false); });
    return () => { alive = false; };
  }, [activeProductRecord, productById]);
  const comparableProducts = activeProductRecord?.comparisonKey
    ? publicProducts.filter((product) => product.comparisonKey === activeProductRecord.comparisonKey)
    : activeProductRecord ? [activeProductRecord] : [];
  const activeProductShop = activeProductRecord ? shopById.get(activeProductRecord.shopId) : undefined;
  const productBackLabel = productOrigin === 'clearance' ? 'Back to clearance deals' : productOrigin === 'shop' ? `Back to ${activeProductShop?.name || 'shop'}` : 'Back to Velnora';

  return (
    <>
      <a className="skip-link" href="#main-content">Skip to content</a>
      <Navbar cart={cart.map(({ product, quantity }) => ({ product, quantity }))} cartCount={cartCount} onQuantityChange={changeCartQuantity} onOpenClearance={openClearanceListing} onOpenCart={() => navigate('/cart')} user={user} onLogout={() => { logout(); navigate('/'); }} />
      <main id="main-content">
        {marketplaceLoading && <p className="api-status-banner" role="status">Loading the Velnora marketplace…</p>}
        {marketplaceError && <p className="api-status-banner api-status-banner--error" role="alert">{marketplaceError}</p>}
        <HeroSection query={productQuery} onQueryChange={setProductQuery} onSearch={submitProductSearch} />

        <section className="category-section page-section" id="categories">
          <Container>
            <div className="section-topline"><SectionHeading eyebrow="Shop your way" title="A good place to start." description="Browse curated independent storefronts by category." /><a className="text-link" href="#shops" onClick={() => { setActiveCategory('all'); setShopQuery(''); }}>Browse all local shops <ArrowRight size={16} /></a></div>
            <div className="category-grid">{categories.map((category) => <CategoryCard key={category.id} category={category} selected={activeCategory === category.id} onSelect={chooseCategory} />)}</div>
          </Container>
        </section>

        <section className="shops-section page-section" id="shops">
          <Container>
            <div className="section-topline"><SectionHeading eyebrow="People behind the places" title="Shops worth discovering." description={activeCategory !== 'all' && !shopQuery ? `Shops in ${categories.find((category) => category.id === activeCategory)?.name}.` : 'Find an independent shop by name or category.'} /><div className="shop-search-field"><Input aria-label="Search shops by name or category" placeholder="Search shops by name or category..." value={shopQuery} onChange={(event) => setShopQuery(event.target.value)} /></div></div>
            {filteredShops.length ? <div className="shop-grid">{filteredShops.map((shop, index) => <ShopCard key={shop.id} shop={shop} priority={index < 4} productCount={publicProducts.filter((product) => product.shopId === shop.id).length} onBrowse={() => openShop(shop.id)} />)}</div> : <Card className="empty-state shop-empty"><span className="empty-icon"><Search size={21} /></span><h3>No shops match “{shopQuery}”</h3><p>Try another shop or category name. Shop search includes every category.</p><Button variant="secondary" onClick={() => { setShopQuery(''); setActiveCategory('all'); }}>Clear shop search</Button></Card>}
          </Container>
        </section>

        <section className="products-section page-section" id="products">
          <Container>
            <div className="section-topline products-topline"><SectionHeading eyebrow="Picked for everyday" title={productQuery ? `Search results for “${productQuery}”` : 'Good things, found locally.'} description="Useful products from independent shops across every category." />{productQuery && <button type="button" className="text-link clear-product-search" onClick={resetProductSearch}>Clear product search <ArrowRight size={16} /></button>}</div>
            {filteredProducts.length ? <div className="product-grid">{filteredProducts.map((product, index) => <ProductCard key={product.id} product={product} priority={index < 4} shop={shopById.get(product.shopId)} onAdd={addToCart} onView={(item) => openProduct(item, 'home')} quantity={cart.find((line) => line.product.id === product.id)?.quantity || 0} />)}</div> : <Card className="empty-state"><span className="empty-icon"><Search size={21} /></span><h3>No local finds just yet</h3><p>Try another product, brand, variant, or pack size.</p><Button variant="secondary" onClick={resetProductSearch}>Clear product search</Button></Card>}
          </Container>
        </section>

        <section className="clearance-section page-section" id="deals">
          <Container>
            <div className="clearance-heading"><div><Badge tone="brass">A little extra value</Badge><h2>Good Finds.<br /><em>Better Prices.</em></h2><p>Lovely local picks at prices worth a second look. No shouting, just good value.</p></div><button type="button" className="clearance-open-button" onClick={openClearanceListing}>View Clearance Deals <ArrowRight size={16} /></button></div>
            <div className="clearance-grid">{clearanceProducts.map((product, index) => <ClearanceCard key={product.id} product={product} priority={index < 4} shop={shopById.get(product.shopId)} onAdd={addToCart} onView={(item) => openProduct(item, 'home')} quantity={cart.find((line) => line.product.id === product.id)?.quantity || 0} />)}</div>
          </Container>
        </section>

        <WhyVelnora />
      </main>
      <Footer />
      {selectedShop && (selectedShop.status || 'Approved') === 'Approved' && !activeProductRecord && <ShopDetailView shop={selectedShop} products={selectedShopProducts} cart={cart} onAdd={addToCart} onViewProduct={(item) => openProduct(item, 'shop')} onClose={() => setActiveShop('')} />}
      {clearanceOpen && !activeProductRecord && <ClearanceDealsView products={clearanceProducts} shopById={shopById} cart={cart} onAdd={addToCart} onViewProduct={(item) => openProduct(item, 'clearance')} onClose={closeClearanceListing} />}
      {activeProductRecord && <ProductDetailView product={activeProductRecord} shop={activeProductShop} comparisons={comparisonResults || comparableProducts} comparisonLoading={comparisonLoading} comparisonError={comparisonError} shopsById={shopById} onBack={() => setActiveProduct('')} backLabel={productBackLabel} onSelectProduct={selectComparedProduct} onAdd={addToCart} quantity={cart.find((line) => line.product.id === activeProductRecord.id)?.quantity || 0} />}
    </>
  );
}

export default function App() {
  const { user, logout, loading: authLoading, authError } = useAuth();
  const cart = useCart();
  const { shops } = useMarketplaceData();
  const currentShop = shops.find((shop) => String(shop.id) === String(cart.shopSwitch?.currentShopId));
  const targetShop = shops.find((shop) => String(shop.id) === String(cart.shopSwitch?.product?.shopId));
  const customerPage = (element) => authLoading ? <PageWait /> : user?.role === 'customer' ? element : <Navigate to="/login" replace />;
  return <>
    {authError && <p className="api-status-banner api-status-banner--error" role="alert">{authError}</p>}
    <Routes>
      <Route path="/cart" element={customerPage(<CartPage />)} />
      <Route path="/checkout" element={customerPage(<CheckoutPage />)} />
      <Route path="/order-summary" element={customerPage(<OrderSummaryPage />)} />
      <Route path="/order-confirmation" element={customerPage(<OrderConfirmationPage />)} />
      <Route path="/orders" element={customerPage(<OrdersPage />)} />
      <Route path="/orders/:orderId" element={customerPage(<OrdersPage />)} />
      <Route path="/account" element={authLoading ? <PageWait /> : user && ['customer', 'shopkeeper'].includes(user.role) ? <AccountPage /> : <Navigate to={user ? '/' : '/login'} replace />} />
      <Route path="/login" element={authLoading ? <PageWait /> : user ? <Navigate to={user.role === 'customer' ? '/' : `/${user.role}`} replace /> : <AuthPage mode="login" />} />
      <Route path="/register" element={authLoading ? <PageWait /> : user ? <Navigate to={user.role === 'customer' ? '/' : `/${user.role}`} replace /> : <AuthPage mode="register" />} />
      <Route path="/forgot-password" element={authLoading ? <PageWait /> : user ? <Navigate to="/" replace /> : <AuthPage mode="forgot" />} />
      <Route path="/shopkeeper" element={authLoading ? <PageWait /> : user?.role === 'shopkeeper' ? <ShopkeeperDashboard /> : <Navigate to={user ? '/' : '/login'} replace />} />
      <Route path="/admin" element={authLoading ? <PageWait /> : user?.role === 'admin' ? <AdminDashboard /> : <Navigate to={user ? '/' : '/login'} replace />} />
      <Route path="*" element={authLoading ? <PageWait /> : user && user.role !== 'customer' ? <Navigate to={`/${user.role}`} replace /> : <MarketplaceHome />} />
    </Routes>
    {cart.error && <div className="api-toast" role="alert"><span>{cart.error}</span><button type="button" aria-label="Dismiss message" onClick={cart.clearError}><X size={16} /></button></div>}
    {cart.shopSwitch && <div className="cart-switch-backdrop"><section className="cart-switch-dialog" role="dialog" aria-modal="true" aria-labelledby="cart-switch-title"><p className="ui-eyebrow">One shop per bag</p><h2 id="cart-switch-title">Start a new bag?</h2><p>Your bag has items from <strong>{currentShop?.name || 'another shop'}</strong>. Clear it and start a new bag for <strong>{targetShop?.name || 'this shop'}</strong>?</p><div><Button variant="secondary" onClick={cart.cancelShopSwitch}>Keep current bag</Button><Button onClick={cart.confirmShopSwitch} disabled={cart.busy}>Clear bag and add item</Button></div></section></div>}
  </>;
}

function PageWait() { return <main className="page-wait" role="status">Verifying your Velnora session…</main>; }
