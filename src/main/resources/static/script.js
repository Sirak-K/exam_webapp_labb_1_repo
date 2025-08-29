// script.js

// Funktion för att hämta väderdata
async function fetchWeather(endpoint) {
  try {
    // Koordinater (exempel: Stockholm). Dessa kan göras dynamiska senare.
    const lon = 18.063240;
    const lat = 59.334591;

    // Visa loading-text/spinner innan fetch
    const article = document.querySelector("main section article");
    article.innerHTML = "<p>Laddar väderdata...</p>";

    // Anropa backend
    const response = await fetch(`/weather/${endpoint}?lon=${lon}&lat=${lat}`);
    if (!response.ok) {
      throw new Error("Fel vid hämtning av väderdata");
    }

    const data = await response.json();

    // Visa resultat i DOM
    article.innerHTML = `
      <p><strong>Tid:</strong> ${data.validTime}</p>
      <p><strong>Temperatur:</strong> ${data.temperature} °C</p>
    `;

  } catch (error) {
    const article = document.querySelector("main section article");
    article.innerHTML = `<p style="color:red;">Kunde inte ladda data: ${error.message}</p>`;
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
