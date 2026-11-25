import { ArrowUpRight, AtSign, MapPin, Mail } from 'lucide-react';

export default function Footer() {
  return <footer className="site-footer" id="footer">
    <div className="ui-container">
      <div className="footer-main">
        <div className="footer-brand"><a href="#top" aria-label="Velnora home"><img src="/velnora-logo-reversed.svg" alt="Velnora" /></a><p>Your Neighbourhood. Better Choices.</p><span>Discover the best of your neighbourhood.</span></div>
        <div className="footer-links"><h2>Explore</h2><a href="#categories">Shop by category</a><a href="#products">Popular near you</a><a href="#deals">Good finds</a><a href="#shops">Local shops</a></div>
        <div className="footer-links"><h2>Here to help</h2><a href="mailto:hello@velnora.in">Customer support</a><a href="mailto:sellers@velnora.in">Become a Seller <ArrowUpRight size={14} /></a><a href="#why-velnora">About Velnora</a><a href="mailto:hello@velnora.in">Contact us</a></div>
        <div className="footer-contact"><h2>Made for local</h2><p><MapPin size={16} /> Starting in Bengaluru, India</p><a href="mailto:hello@velnora.in"><Mail size={16} /> Customer support</a><a href="#footer" aria-label="Velnora on Instagram"><AtSign size={17} /> Instagram</a></div>
      </div>
      <div className="footer-bottom"><span>© {new Date().getFullYear()} Velnora. All rights reserved.</span><span>Thoughtfully local, always.</span></div>
    </div>
  </footer>;
}
