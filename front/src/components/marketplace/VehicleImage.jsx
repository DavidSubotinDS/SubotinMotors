export default function VehicleImage({ src, alt, fallback }) {
  if (src) {
    return <img className="summary-image" src={src} alt={alt} loading="lazy" />;
  }

  return (
    <div className="summary-image summary-image-fallback" aria-hidden="true">
      {fallback}
    </div>
  );
}
