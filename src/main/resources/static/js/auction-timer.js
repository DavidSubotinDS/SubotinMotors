(() => {
  const timers = document.querySelectorAll("[data-auction-end]");

  function updateTimer(container) {
    const end = Number(container.dataset.auctionEnd);
    const countdown = container.querySelector(".auction-countdown");
    if (!end || !countdown) {
      return;
    }

    const remaining = end - Date.now();
    container.classList.toggle("auction-ending-soon", remaining > 0 && remaining <= 24 * 60 * 60 * 1000);
    container.classList.toggle("auction-urgent", remaining > 0 && remaining <= 60 * 60 * 1000);
    container.classList.toggle("auction-ended", remaining <= 0);

    if (remaining <= 0) {
      countdown.textContent = "Auction ended";
      return;
    }

    const totalSeconds = Math.floor(remaining / 1000);
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    countdown.textContent = `${days}d ${hours}h ${minutes}m ${seconds}s remaining`;
  }

  function updateAll() {
    timers.forEach(updateTimer);
  }

  updateAll();
  window.setInterval(updateAll, 1000);
})();
