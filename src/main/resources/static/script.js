// script.js

// Funktion för att hämta väderdata
async function fetchWeather(endpoint) {
  try {
    const lon = 18.063240;  // Stockholm koordinater (kan ändras)
    const lat = 59.334591;

    const article = document.querySelector("main section article");
    const aside = document.querySelector("main section aside");

    // Visa spinner innan fetch
    article.innerHTML = '<div class="spinner"></div>';
    aside.innerHTML = "";

    // Hämta data från backend
    const response = await fetch(`/weather/${endpoint}?lon=${lon}&lat=${lat}`);
    if (!response.ok) {
      throw new Error("Fel vid hämtning av väderdata");
    }

    const data = await response.json();

    // Validera datumsträngen
    const cleanValidTime = data.validTime.trim();
    let dateObj = new Date(cleanValidTime);

    // Fallback om datumet är ogiltigt
    if (isNaN(dateObj.getTime())) {
      aside.innerHTML = `
        <p><strong>Datum:</strong> Ogiltigt datum</p>
        <p><strong>Tid:</strong> Ogiltig tid</p>
      `;
      article.innerHTML = `<p><strong>Temperatur:</strong> ${data.temperature} °C</p>`;
      return;
    }

    const dateStr = dateObj.toLocaleDateString("sv-SE", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    });
    const timeStr = dateObj.toLocaleTimeString("sv-SE", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false
    });

    // Visa temperatur i article
    article.innerHTML = `
      <p><strong>Temperatur:</strong> ${data.temperature} °C</p>
    `;

    // Visa datum & tid i aside
    aside.innerHTML = `
      <p><strong>Datum:</strong> ${dateStr}</p>
      <p><strong>Tid:</strong> ${timeStr}</p>
    `;

  } catch (error) {
    const article = document.querySelector("main section article");
    const aside = document.querySelector("main section aside");

    article.innerHTML = `<p style="color:red;">Kunde inte ladda data: ${error.message}</p>`;
    aside.innerHTML = "";
  }
}

// Kör rätt fetch beroende på vilken sida vi är på
document.addEventListener("DOMContentLoaded", () => {
  const path = window.location.pathname;

  if (path.endsWith("index.html") || path === "/") {
    fetchWeather("today");
  } else if (path.endsWith("weather_yesterday.html")) {
    fetchWeather("yesterday");
  } else if (path.endsWith("weather_tomorrow.html")) {
    fetchWeather("tomorrow");
  }
});
