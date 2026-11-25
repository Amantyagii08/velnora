import { ArrowDown, ArrowRight, Search, Sparkles } from 'lucide-react';

export default function HeroSection({ query, onQueryChange, onSearch }) {
  return (
    <section className="hero-section" id="top">
      <div className="hero-inner ui-container">
        <div className="hero-copy">
          <span className="hero-kicker"><Sparkles size={15} strokeWidth={1.8} /> A little closer to home</span>
          <h1>Good things,<br /><em>right around you.</em></h1>
          <p>Meet the shops on your street. Find what you need, compare local prices, and keep good things close.</p>
          <form className="hero-search" onSubmit={onSearch} role="search">
            <Search size={20} strokeWidth={1.8} aria-hidden="true" />
            <input value={query} onChange={(event) => onQueryChange(event.target.value)} placeholder="Try coffee, mangoes, or a brand" aria-label="Search products by name or brand" />
            {query && <button type="button" className="search-clear" onClick={() => onQueryChange('')} aria-label="Clear search">Clear</button>}
            <button className="hero-search-submit" type="submit">Explore <ArrowRight size={17} /></button>
          </form>
          <div className="hero-quick-links"><span>Popular nearby</span><a href="#products" onClick={() => onQueryChange('mango')}>Mangoes</a><a href="#products" onClick={() => onQueryChange('coffee')}>Filter coffee</a><a href="#shops">Local shops</a></div>
        </div>
        <div className="hero-art" aria-label="Fresh seasonal mangoes from a local shop">
          <img src="https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&w=1400&q=90" alt="Ripe mangoes picked for the season" onError={(event) => { event.currentTarget.style.display = 'none'; }} />
          <div className="hero-art-shade" />
          <div className="hero-stamp"><span>From your</span><strong>neighbourhood</strong><span>with care</span></div>
          <div className="hero-art-caption"><span>Seasonal and close by</span><a href="#deals">See local finds <ArrowRight size={15} /></a></div>
        </div>
      </div>
      <a className="hero-scroll" href="#categories"><ArrowDown size={15} /> Discover what’s nearby</a>
    </section>
  );
}
