import { ArrowRight, Compass, HeartHandshake, MapPinned, Tags } from 'lucide-react';

const values = [
  { icon: Compass, title: 'Discover close by', text: 'Find the neighbourhood businesses that make your area feel like home.' },
  { icon: Tags, title: 'Compare with ease', text: 'See useful choices and prices from local shops in one place.' },
  { icon: MapPinned, title: 'Find a good deal', text: 'Pick up thoughtful local offers without the noise.' },
  { icon: HeartHandshake, title: 'Keep good things local', text: 'Every order helps independent shopkeepers keep doing what they do best.' },
];

export default function WhyVelnora() {
  return <section className="why-section" id="why-velnora">
    <div className="ui-container"><div className="why-top"><div><p className="eyebrow">Local has its advantages</p><h2>Good Finds.<br /><em>Better Prices.</em></h2></div><p>Thoughtful shopping starts nearby. Velnora brings your local shops a little closer, so good choices are easier to find.</p></div>
      <div className="why-grid">{values.map(({ icon: Icon, title, text }, index) => <article className="why-value" key={title}><span className="why-icon"><Icon size={23} strokeWidth={1.65} /></span><span className="why-number">0{index + 1}</span><h3>{title}</h3><p>{text}</p></article>)}</div>
      <a className="why-cta" href="#shops">Meet your local shops <ArrowRight size={17} /></a>
    </div>
  </section>;
}
