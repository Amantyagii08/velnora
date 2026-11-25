import { ArrowDown, ArrowRight, Search, Sparkles } from 'lucide-react';

export default function HeroSection({ query, onQueryChange, onSearch }) {
  return (
    <section className="hero-section" id="top">
      <div className="hero-inner ui-container">
        <div className="hero-copy">
          <span className="hero-kicker"><Sparkles size={15} strokeWidth={1.8} /> A little closer to home</span>
          <h1>Good things,<br /><em>right around you.</em></h1>
          <p>Find what you need, compare clear prices, and discover independent shops worth knowing.</p>
          <form className="hero-search" onSubmit={onSearch} role="search">
            <Search size={20} strokeWidth={1.8} aria-hidden="true" />
            <input value={query} onChange={(event) => onQueryChange(event.target.value)} placeholder="Try coffee, mangoes, or a brand" aria-label="Search products by name or brand" />
            {query && <button type="button" className="search-clear" onClick={() => onQueryChange('')} aria-label="Clear search">Clear</button>}
            <button className="hero-search-submit" type="submit">Explore <ArrowRight size={17} /></button>
          </form>
          <div className="hero-quick-links"><span>Popular picks</span><a href="#products" onClick={() => onQueryChange('mango')}>Mangoes</a><a href="#products" onClick={() => onQueryChange('coffee')}>Filter coffee</a><a href="#shops">Browse shops</a></div>
        </div>
        <div className="hero-art" aria-label="Local shopkeepers and neighbors connecting in their neighborhood">
          <img src="/images/local-neighborhood-hero.png" alt="A local shopkeeper handing a grocery bag to a neighbor, with nearby shops and residents along the street" loading="eager" fetchPriority="high" decoding="async" onError={(event) => { event.currentTarget.style.display = 'none'; }} />
          <div className="hero-art-shade" />
          <div className="hero-stamp"><span className="hero-stamp-dot" aria-hidden="true" /><span>Seasonal Harvest • Curated Local</span></div>
          <div className="hero-art-caption"><span>Seasonal and close by</span><a href="#deals">See local finds <ArrowRight size={15} /></a></div>
        </div>
      </div>
      <a className="hero-scroll" href="#categories" aria-label="Browse categories"><ArrowDown size={15} aria-hidden="true" /></a>
    </section>
  );
}
