import { ArrowRight, Compass, HeartHandshake, MapPinned, Tags } from 'lucide-react';
import { useEffect, useRef } from 'react';

const values = [
  { icon: Compass, title: 'Find shops worth knowing', text: 'Discover independent stores with distinctive finds.' },
  { icon: Tags, title: 'Compare with clarity', text: 'See useful choices and prices side by side.' },
  { icon: MapPinned, title: 'Spot a good offer', text: 'Browse considered prices without the noise.' },
  { icon: HeartHandshake, title: 'Support independent stores', text: 'Find products from the people behind the shop.' },
];

export default function WhyVelnora() {
  const taglineRef = useRef(null);
  useEffect(() => {
    const words = taglineRef.current?.querySelectorAll('[data-tagline-word]');
    if (!words?.length || !('IntersectionObserver' in window)) return;
    const observer = new IntersectionObserver((entries) => entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-active');
        observer.unobserve(entry.target);
      }
    }), { threshold: 0.7, rootMargin: '-42% 0px -42% 0px' });
    words.forEach((word) => observer.observe(word));
    return () => observer.disconnect();
  }, []);

  return <section className="why-section" id="why-velnora">
    <div className="ui-container"><div className="why-top"><div><p className="eyebrow">A better way to choose</p><h2>Good Finds.<br /><em>Better Prices.</em></h2></div><p>Browse independent stores, compare prices, and find thoughtful offers in one place.</p></div>
      <p className="brand-tagline-reveal" ref={taglineRef} aria-label="Your Neighbourhood. Better Choices."><span data-tagline-word>Your</span>{' '}<span data-tagline-word>Neighbourhood.</span><br /><span data-tagline-word>Better</span>{' '}<span data-tagline-word>Choices.</span></p>
      <div className="why-grid">{values.map(({ icon: Icon, title, text }, index) => <article className="why-value" key={title}><span className="why-icon"><Icon size={23} strokeWidth={1.65} /></span><span className="why-number">0{index + 1}</span><h3>{title}</h3><p>{text}</p></article>)}</div>
      <a className="why-cta" href="#shops">Explore the shops <ArrowRight size={17} /></a>
    </div>
  </section>;
}
