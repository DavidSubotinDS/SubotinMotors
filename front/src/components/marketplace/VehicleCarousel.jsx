import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

import VehicleImage from './VehicleImage.jsx';

export default function VehicleCarousel({
  images,
  src,
  alt,
  fallback,
  intervalMs = 5000,
}) {
  const imageUrls = useMemo(() => {
    const candidates = [...(Array.isArray(images) ? images : []), src].filter(Boolean);
    return [...new Set(candidates)];
  }, [images, src]);
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const reducedMotion = globalThis.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

  useEffect(() => {
    setIndex((current) => Math.min(current, Math.max(imageUrls.length - 1, 0)));
  }, [imageUrls.length]);

  useEffect(() => {
    if (paused || reducedMotion || imageUrls.length < 2) return undefined;
    const timer = globalThis.setInterval(() => {
      setIndex((current) => (current + 1) % imageUrls.length);
    }, intervalMs);
    return () => globalThis.clearInterval(timer);
  }, [imageUrls.length, intervalMs, paused, reducedMotion]);

  function showPrevious() {
    setIndex((current) => (current - 1 + imageUrls.length) % imageUrls.length);
  }

  function showNext() {
    setIndex((current) => (current + 1) % imageUrls.length);
  }

  const hasGallery = imageUrls.length > 1;
  const currentSrc = imageUrls[index] ?? null;

  return (
    <div
      className="vehicle-carousel"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setPaused(false);
      }}
    >
      <VehicleImage
        src={currentSrc}
        alt={hasGallery ? `${alt} — image ${index + 1} of ${imageUrls.length}` : alt}
        fallback={fallback}
      />
      {hasGallery && (
        <>
          <button className="carousel-control carousel-previous" type="button" onClick={showPrevious} aria-label="Previous image">
            <ChevronLeft aria-hidden="true" size={22} />
          </button>
          <button className="carousel-control carousel-next" type="button" onClick={showNext} aria-label="Next image">
            <ChevronRight aria-hidden="true" size={22} />
          </button>
          <div className="carousel-footer">
            <span className="carousel-count">{index + 1} / {imageUrls.length}</span>
            <div className="carousel-dots" aria-label="Choose vehicle image">
              {imageUrls.map((image, imageIndex) => (
                <button
                  className={imageIndex === index ? 'is-active' : undefined}
                  type="button"
                  key={image}
                  onClick={() => setIndex(imageIndex)}
                  aria-label={`Show image ${imageIndex + 1}`}
                  aria-current={imageIndex === index ? 'true' : undefined}
                />
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
