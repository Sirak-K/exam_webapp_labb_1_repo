// script.js

// Hjälpfunktion för att rendera väderdata
function renderWeather(data, endpoint) {
  const article = document.querySelector("main section article");
  const aside = document.querySelector("main section aside");

  // Hämta ren tid utan label
  const cleanValidTime = data.validTime.split(" (")[0];
  const label = data.validTime.includes("(") ? data.validTime.split(" (")[1].replace(")", "") : "";

  const dateObj = new Date(cleanValidTime);
  if (isNaN(dateObj.getTime())) {
    article.innerHTML = `<p><strong>Temperatur:</strong> ${data.temperature} °C</p>`;
    aside.innerHTML = `<p><strong>Datum/Tid:</strong> (ogiltigt datum)</p>`;
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

  article.innerHTML = `<p><strong>Temperatur:</strong> ${data.temperature} °C</p>`;
  aside.innerHTML = `
    <p><strong>Datum:</strong> ${dateStr}</p>
    <p><strong>Tid:</strong> ${timeStr}</p>
    ${endpoint === "yesterday" && label ? `<p style="font-size:0.9em;color:#666;">(${label})</p>` : ""}
  `;
}

// Hjälpfunktion för fel
function showError(error) {
  const article = document.querySelector("main section article");
  const aside = document.querySelector("main section aside");
  article.innerHTML = `<p style="color:red;">Kunde inte ladda data: ${error.message}</p>`;
  aside.innerHTML = "";
}

// Funktion för att hämta väderdata
async function fetchWeather(endpoint) {
  const article = document.querySelector("main section article");
  const aside = document.querySelector("main section aside");

  // Visa spinner
  article.innerHTML = '<div class="spinner"></div>';
  aside.innerHTML = "";

  try {
    // Uppdaterad till nya RESTful endpoints
    let url = `/api/weather/${endpoint}`;

    // Bara idag och imorgon kräver lon/lat
    if (endpoint === "today" || endpoint === "tomorrow" || endpoint === "forecast") {
      const lon = 18.063240;
      const lat = 59.334591;
      url += `?lon=${lon}&lat=${lat}`;
    }

    const response = await fetch(url);
    if (!response.ok) throw new Error("Fel vid hämtning av väderdata");

    const data = await response.json();
    renderWeather(data, endpoint);

  } catch (error) {
    showError(error);
  }
}

// Initiering
document.addEventListener("DOMContentLoaded", () => {
  // === Hamburger meny toggle ===
  const menuToggle = document.querySelector(".menu-toggle");
  const siteNav = document.querySelector(".site-nav");

  if (menuToggle && siteNav) {
    // Toggle öppen/stängd
    menuToggle.addEventListener("click", () => {
      menuToggle.classList.toggle("open");
      siteNav.classList.toggle("open");
    });

    // Stäng menyn automatiskt när man klickar på en länk
    const navLinks = document.querySelectorAll(".site-nav a");
    navLinks.forEach(link => {
      link.addEventListener("click", () => {
        menuToggle.classList.remove("open");
        siteNav.classList.remove("open");
      });
    });
  }

  // === Väderhämtning beroende på sida ===
  const path = window.location.pathname;

  if (path.endsWith("index.html") || path === "/") {
    fetchWeather("today");
  } else if (path.endsWith("weather_yesterday.html")) {
    fetchWeather("yesterday");
  } else if (path.endsWith("weather_tomorrow.html")) {
    fetchWeather("tomorrow");
  }
});
