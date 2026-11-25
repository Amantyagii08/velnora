import { useMemo, useState } from 'react';
import { ArrowRight, Search } from 'lucide-react';
import { Badge, Button, Card, Container, Input, SectionHeading } from './components/ui.jsx';
import Navbar from './components/homepage/Navbar.jsx';
import HeroSection from './components/homepage/HeroSection.jsx';
import CategoryCard from './components/homepage/CategoryCard.jsx';
import ProductCard from './components/homepage/ProductCard.jsx';
import ShopCard from './components/homepage/ShopCard.jsx';
import ClearanceCard from './components/homepage/ClearanceCard.jsx';
import ShopDetailView from './components/homepage/ShopDetailView.jsx';
import ClearanceDealsView from './components/homepage/ClearanceDealsView.jsx';
import WhyVelnora from './components/homepage/WhyVelnora.jsx';
import Footer from './components/homepage/Footer.jsx';
import { categories, products, shops } from './data/marketplace.js';
import './App.css';

const shopById = new Map(shops.map((shop) => [shop.id, shop]));
const popularShops = [...shops].sort((a, b) => Number(b.rating) - Number(a.rating));

export default function App() {
  const [productQuery, setProductQuery] = useState('');
  const [shopQuery, setShopQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');
  const [activeShop, setActiveShop] = useState('');
  const [clearanceOpen, setClearanceOpen] = useState(false);
  const [cart, setCart] = useState([]);

  const filteredProducts = useMemo(() => {
    const query = productQuery.trim().toLowerCase();
    if (!query) return products;
    return products.filter((product) => [product.name, product.brand, product.variant, product.unit]
      .some((value) => value?.toLowerCase().includes(query)));
  }, [productQuery]);

  const filteredShops = useMemo(() => {
    const query = shopQuery.trim().toLowerCase();
    return popularShops.filter((shop) => {
      const matchesSearch = !query || `${shop.name} ${shop.category}`.toLowerCase().includes(query);
      const matchesCategory = Boolean(query) || activeCategory === 'all' || shop.categoryId === activeCategory;
      return matchesSearch && matchesCategory;
    });
  }, [shopQuery, activeCategory]);

  const selectedShop = shopById.get(activeShop);
  const selectedShopProducts = selectedShop ? products.filter((product) => product.shopId === selectedShop.id) : [];
  const cartCount = cart.reduce((count, line) => count + line.quantity, 0);
  const clearanceProducts = products.filter((product) => product.clearance);

  function addToCart(product) {
    setCart((current) => {
      const existing = current.find((line) => line.product.id === product.id);
      return existing
        ? current.map((line) => line.product.id === product.id ? { ...line, quantity: line.quantity + 1 } : line)
        : [...current, { product, quantity: 1 }];
    });
  }

  function changeCartQuantity(productId, change) {
    setCart((current) => current
      .map((line) => line.product.id === productId ? { ...line, quantity: line.quantity + change } : line)
      .filter((line) => line.quantity > 0));
  }

  function chooseCategory(categoryId) {
    setActiveCategory((current) => current === categoryId ? 'all' : categoryId);
    setShopQuery('');
    document.getElementById('shops')?.scrollIntoView({ behavior: 'smooth' });
  }

  function openShop(shopId) {
    setActiveShop(shopId);
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
    setClearanceOpen(true);
  }

  return (
    <>
      <Navbar cart={cart} cartCount={cartCount} onQuantityChange={changeCartQuantity} onOpenClearance={openClearanceListing} />
      <main>
        <HeroSection query={productQuery} onQueryChange={setProductQuery} onSearch={submitProductSearch} />

        <section className="category-section page-section" id="categories">
          <Container>
            <div className="section-topline"><SectionHeading eyebrow="Shop your way" title="A good place to start." description="Choose a category to discover the independent shops in your neighbourhood." /><a className="text-link" href="#shops" onClick={() => { setActiveCategory('all'); setShopQuery(''); }}>Browse all local shops <ArrowRight size={16} /></a></div>
            <div className="category-grid">{categories.map((category) => <CategoryCard key={category.id} category={category} selected={activeCategory === category.id} onSelect={chooseCategory} />)}</div>
          </Container>
        </section>

        <section className="shops-section page-section" id="shops">
          <Container>
            <div className="section-topline"><SectionHeading eyebrow="People behind the places" title="Popular Local Shops" description={activeCategory !== 'all' && !shopQuery ? `Shops in ${categories.find((category) => category.id === activeCategory)?.name}.` : 'Search local businesses by shop name or category.'} /><div className="shop-search-field"><Input aria-label="Search shops by name or category" placeholder="Search shops by name or category..." value={shopQuery} onChange={(event) => setShopQuery(event.target.value)} /></div></div>
            {filteredShops.length ? <div className="shop-grid">{filteredShops.map((shop) => <ShopCard key={shop.id} shop={shop} productCount={products.filter((product) => product.shopId === shop.id).length} onBrowse={() => openShop(shop.id)} />)}</div> : <Card className="empty-state shop-empty"><span className="empty-icon"><Search size={21} /></span><h3>No shops match “{shopQuery}”</h3><p>Try another shop or category name. Shop search includes every category.</p><Button variant="secondary" onClick={() => { setShopQuery(''); setActiveCategory('all'); }}>Clear shop search</Button></Card>}
          </Container>
        </section>

        <section className="products-section page-section" id="products">
          <Container>
            <div className="section-topline products-topline"><SectionHeading eyebrow="Picked close by" title={productQuery ? `Search results for “${productQuery}”` : 'Good things, found locally.'} description="Popular products from local shops across every category." />{productQuery && <button type="button" className="text-link clear-product-search" onClick={resetProductSearch}>Clear product search <ArrowRight size={16} /></button>}</div>
            {filteredProducts.length ? <div className="product-grid">{filteredProducts.map((product) => <ProductCard key={product.id} product={product} shop={shopById.get(product.shopId)} onAdd={addToCart} quantity={cart.find((line) => line.product.id === product.id)?.quantity || 0} />)}</div> : <Card className="empty-state"><span className="empty-icon"><Search size={21} /></span><h3>No local finds just yet</h3><p>Try another product, brand, variant, or pack size.</p><Button variant="secondary" onClick={resetProductSearch}>Clear product search</Button></Card>}
          </Container>
        </section>

        <section className="clearance-section page-section" id="deals">
          <Container>
            <div className="clearance-heading"><div><Badge tone="brass">A little extra value</Badge><h2>Good Finds.<br /><em>Better Prices.</em></h2><p>Lovely local picks at prices worth a second look. No shouting, just good value.</p></div><button type="button" className="clearance-open-button" onClick={openClearanceListing}>View Clearance Deals <ArrowRight size={16} /></button></div>
            <div className="clearance-grid">{clearanceProducts.map((product) => <ClearanceCard key={product.id} product={product} shop={shopById.get(product.shopId)} onAdd={addToCart} quantity={cart.find((line) => line.product.id === product.id)?.quantity || 0} />)}</div>
          </Container>
        </section>

        <WhyVelnora />
      </main>
      <Footer />
      {selectedShop && <ShopDetailView shop={selectedShop} products={selectedShopProducts} cart={cart} onAdd={addToCart} onClose={() => setActiveShop('')} />}
      {clearanceOpen && <ClearanceDealsView products={clearanceProducts} shopById={shopById} cart={cart} onAdd={addToCart} onClose={() => setClearanceOpen(false)} />}
    </>
  );
}
