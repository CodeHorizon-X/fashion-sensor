const form = document.getElementById("fashionForm");
const photoInput = document.getElementById("photo");
const previewContainer = document.getElementById("previewContainer");
const imagePreview = document.getElementById("imagePreview");
const fileMeta = document.getElementById("fileMeta");
const submitButton = document.getElementById("submitButton");
const loadingSpinner = document.getElementById("loadingSpinner");
const buttonLabel = submitButton?.querySelector(".button-label");
const formStatus = document.getElementById("formStatus");
const exploreGrid = document.getElementById("exploreGrid");
const activeFilterTitle = document.getElementById("activeFilterTitle");
const activeFilterDescription = document.getElementById("activeFilterDescription");
const outfitHeading = document.getElementById("outfitHeading");
const resultStyle = document.getElementById("resultStyle");
const resultSource = document.getElementById("resultSource");
const activeAudienceChip = document.getElementById("activeAudienceChip");
const itemsList = document.getElementById("itemsList");
const outfitCards = document.getElementById("outfitCards");
const pinterestLink = document.getElementById("pinterestLink");
const pinterestHeading = document.getElementById("pinterestHeading");
const pinterestDescription = document.getElementById("pinterestDescription");
const pinterestContainer = document.getElementById("pinterest-container");
const audienceSelect = document.getElementById("audience");
const styleSelect = document.getElementById("style");
const previousOutfitButton = document.getElementById("previousOutfitButton");
const nextOutfitButton = document.getElementById("nextOutfitButton");
const rethinkOutfitButton = document.getElementById("rethinkOutfitButton");
const globalSearch = document.getElementById("globalSearch");
const themeToggle = document.getElementById("themeToggle");
const themeIcon = document.getElementById("themeIcon");

const API_URL = "http://localhost:8080/api/suggest";
let previewUrl = null;

const uiState = {
  section: "home",
  audience: audienceSelect?.value || "men",
  style: "all",
  resultSet: null,
  activeOutfitIndex: 0,
  formPayload: null,
};

const exploreCards = [
  { title: "Off-Duty Layers", description: "Relaxed silhouettes with clean sneakers and soft neutrals for polished daily wear.", audience: "men", style: "casual", image: "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=900&q=80" },
  { title: "Minimal Tailoring", description: "Structured basics, monochrome layers, and calm textures for sharp modern dressing.", audience: "men", style: "minimal", image: "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80" },
  { title: "Gen Z Street Mix", description: "Boxy fits, denim volume, and statement sneakers for trend-first styling.", audience: "men", style: "genz", image: "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=900&q=80" },
  { title: "Refined Work Edit", description: "Crisp separates, darker tailoring, and smart footwear for office-ready confidence.", audience: "men", style: "formal", image: "https://images.unsplash.com/photo-1507679799987-c73779587ccf?auto=format&fit=crop&w=900&q=80" },
  { title: "Weekend Ease", description: "Easy denim, neutral tops, and fresh sneakers for versatile city plans.", audience: "women", style: "casual", image: "https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=900&q=80" },
  { title: "Soft Minimalism", description: "Cream tones, clean lines, and elevated essentials for understated luxury.", audience: "women", style: "minimal", image: "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=900&q=80" },
  { title: "Trending Campus Fit", description: "Cropped layers, wide-leg denim, and playful sneakers with Gen Z energy.", audience: "women", style: "genz", image: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=900&q=80" },
  { title: "Power Dressing", description: "Tailored blazers and sleek heels for a work look that still feels current.", audience: "women", style: "formal", image: "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=900&q=80" },
  { title: "Playground Cool", description: "Comfy layers, soft textures, and colorful sneakers designed for movement.", audience: "kids", style: "casual", image: "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?auto=format&fit=crop&w=900&q=80" },
  { title: "Mini Streetwear", description: "Oversized hoodies, joggers, and bold trainers for trend-led kids styling.", audience: "kids", style: "genz", image: "https://images.unsplash.com/photo-1516627145497-ae6968895b74?auto=format&fit=crop&w=900&q=80" },
  { title: "Clean Occasion Wear", description: "Neat shirts, tailored bottoms, and polished shoes for family events.", audience: "kids", style: "formal", image: "https://images.unsplash.com/photo-1519340241574-2cec6aef0c01?auto=format&fit=crop&w=900&q=80" },
  { title: "Soft Minimal Basics", description: "Muted colors and simple silhouettes that feel tidy but playful.", audience: "kids", style: "minimal", image: "https://images.unsplash.com/photo-1503919545889-aef636e10ad4?auto=format&fit=crop&w=900&q=80" },
];

const pinterestDescriptors = {
  men: ["layered neutrals", "smart casual", "streetwear sneakers"],
  women: ["denim chic", "minimal elegance", "trend-led styling"],
  kids: ["playful layers", "mini streetwear", "smart family looks"],
};

initialize();

function initialize() {
  renderExploreGrid();
  renderResultSet(buildDefaultResultSet());
  bindSectionNavigation();
  bindFilters();
  bindFormSync();
  bindResultControls();

  photoInput?.addEventListener("change", handleImagePreview);
  form?.addEventListener("submit", handleSubmit);
  globalSearch?.addEventListener("input", handleSearch);
  globalSearch?.addEventListener("keydown", handleSearch);
  themeToggle?.addEventListener("click", toggleTheme);
}

function bindSectionNavigation() {
  document.querySelectorAll("[data-section-target]").forEach((button) => {
    button.addEventListener("click", () => setActiveSection(button.dataset.sectionTarget));
  });

  document.querySelectorAll('a[href^="#"]').forEach((link) => {
    link.addEventListener("click", (event) => {
      const id = link.getAttribute("href")?.slice(1);
      const target = id ? document.getElementById(id) : null;
      if (!target) {
        return;
      }

      event.preventDefault();
      setActiveSection(id);
    });
  });
}

function bindFilters() {
  document.querySelectorAll("[data-filter-group]").forEach((button) => {
    button.addEventListener("click", () => {
      const { filterGroup, filterValue } = button.dataset;
      if (!filterGroup || !filterValue) {
        return;
      }

      uiState[filterGroup] = filterValue;
      updateFilterButtons(filterGroup, filterValue);

      if (filterGroup === "audience" && audienceSelect) {
        audienceSelect.value = filterValue;
      }

      if (filterGroup === "style" && styleSelect && filterValue !== "all") {
        styleSelect.value = filterValue;
      }

      renderExploreGrid();
      renderResultSet(deriveFilteredResultSet());
      loadPinterestImages(buildPinterestQueryFromState(uiState.style === "all" ? "casual" : uiState.style, uiState.audience));
    });
  });
}

function bindFormSync() {
  audienceSelect?.addEventListener("change", () => {
    uiState.audience = audienceSelect.value;
    updateFilterButtons("audience", uiState.audience);
    renderExploreGrid();
    renderResultSet(deriveFilteredResultSet());
    loadPinterestImages(buildPinterestQueryFromState(uiState.style === "all" ? "casual" : uiState.style, uiState.audience));
  });

  styleSelect?.addEventListener("change", () => {
    uiState.style = styleSelect.value;
    updateFilterButtons("style", uiState.style);
    renderExploreGrid();
    renderResultSet(deriveFilteredResultSet());
    loadPinterestImages(buildPinterestQueryFromState(uiState.style === "all" ? "casual" : uiState.style, uiState.audience));
  });
}

function bindResultControls() {
  previousOutfitButton?.addEventListener("click", () => shiftOutfit(-1));
  nextOutfitButton?.addEventListener("click", () => shiftOutfit(1));
  rethinkOutfitButton?.addEventListener("click", handleRethinkOutfit);
}

function handleImagePreview(event) {
  const [file] = event.target.files || [];

  if (previewUrl) {
    URL.revokeObjectURL(previewUrl);
    previewUrl = null;
  }

  if (!file) {
    previewContainer.classList.add("hidden");
    imagePreview.removeAttribute("src");
    fileMeta.textContent = "";
    return;
  }

  previewUrl = URL.createObjectURL(file);
  imagePreview.src = previewUrl;
  fileMeta.textContent = `${file.name} • ${formatFileSize(file.size)}`;
  previewContainer.classList.remove("hidden");
}

async function handleSubmit(event) {
  event.preventDefault();
  const formData = new FormData(form);

  // Sync current UI state explicitly to formData
  formData.set("audience", uiState.audience);
  formData.set("style", uiState.style);
  uiState.formPayload = new FormData(formData);

  setLoadingState(true);
  setStatus("Creating your AI styling board...", "success");
  setActiveSection("results");

  // Add skeleton loaders to result panels
  outfitCards.innerHTML = '<div class="skeleton" style="height: 120px; border-radius: 1.5rem; width: 100%;"></div>'.repeat(3);
  itemsList.innerHTML = '<div class="skeleton" style="height: 80px; border-radius: 1.4rem; width: 100%;"></div>'.repeat(4);

  try {
    const response = await fetch(API_URL, {
      method: "POST",
      body: formData,
    });

    const json = await response.json();
    if (!response.ok) {
      throw new Error(json.error || `Server error (${response.status})`);
    }

    uiState.resultSet = normalizeResultSet(json, formData);
    uiState.activeOutfitIndex = 0;
    renderResultSet(deriveFilteredResultSet());
    loadPinterestImages(uiState.resultSet.pinterestQuery);
    setStatus("Outfit options generated successfully.", "success");
    document.getElementById("results")?.scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (error) {
    uiState.resultSet = buildDefaultResultSet();
    uiState.activeOutfitIndex = 0;
    renderResultSet(deriveFilteredResultSet());
    loadPinterestImages(uiState.resultSet.pinterestQuery);
    setStatus(error.message || "Something went wrong while contacting the server.", "error");
  } finally {
    setLoadingState(false);
  }
}

async function handleRethinkOutfit() {
  const activeSet = deriveFilteredResultSet();
  if (uiState.formPayload) {
    const outfits = activeSet.outfits || [];
    if (uiState.activeOutfitIndex < outfits.length - 1) {
      shiftOutfit(1);
      return;
    }

    setLoadingState(true);
    setStatus("Generating another outfit set...", "success");

    try {
      const response = await fetch(API_URL, {
        method: "POST",
        body: new FormData(uiState.formPayload),
      });
      const json = await response.json();
      if (!response.ok) {
        throw new Error(json.error || `Server error (${response.status})`);
      }

      uiState.resultSet = normalizeResultSet(json, uiState.formPayload);
      uiState.activeOutfitIndex = 0;
      renderResultSet(deriveFilteredResultSet());
      loadPinterestImages(uiState.resultSet.pinterestQuery);
      setStatus("Fresh outfit options are ready.", "success");
    } catch (error) {
      shiftOutfit(1);
      setStatus(error.message || "Unable to fetch a new outfit set right now.", "error");
    } finally {
      setLoadingState(false);
    }
    return;
  }

  shiftOutfit(1);
}

function shiftOutfit(direction) {
  const activeSet = deriveFilteredResultSet();
  if (!activeSet.outfits.length) {
    return;
  }

  const size = activeSet.outfits.length;
  uiState.activeOutfitIndex = (uiState.activeOutfitIndex + direction + size) % size;
  renderResultSet(activeSet);
}

function handleSearch(event) {
  // If Enter key is pressed, execute live Unsplash search
  if (event.type === 'keydown' && event.key !== 'Enter') return;
  if (event.type === 'keydown' && event.key === 'Enter') {
    event.preventDefault();
    const query = event.target.value.trim();
    if (!query) return;
    
    // Switch to Explore section to see live results
    setActiveSection("explore");
    activeFilterTitle.textContent = "Live Search";
    activeFilterDescription.textContent = `Showing real-time results for "${query}"`;
    
    fetchUnsplashImages(query + " fashion outfit");
    return;
  }

  // Filter fallback
  const query = event.target.value.toLowerCase().trim();
  
  // Filter Explore Data if on explore screen
  document.querySelectorAll("#exploreGrid .product-card").forEach((card) => {
    const text = card.textContent.toLowerCase();
    card.style.display = text.includes(query) ? "" : "none";
  });

  // Filter Items and Outfits if on results screen
  document.querySelectorAll("#itemsList .item-card").forEach((card) => {
    const text = card.textContent.toLowerCase();
    card.style.display = text.includes(query) ? "flex" : "none";
  });

  document.querySelectorAll("#outfitCards .outfit-option-card").forEach((card) => {
    const text = card.textContent.toLowerCase();
    card.style.display = text.includes(query) ? "block" : "none";
  });
}

function toggleTheme() {
  document.body.classList.toggle("light-mode");
  const isLight = document.body.classList.contains("light-mode");
  
  if (isLight) {
    // Sun Icon
    themeIcon.innerHTML = `<circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>`;
  } else {
    // Moon Icon
    themeIcon.innerHTML = `<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>`;
  }
}

function setActiveSection(sectionId) {
  uiState.section = sectionId;

  document.querySelectorAll("[data-section-target]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.sectionTarget === sectionId);
  });

  document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function applyNegativeConstraints(baseQuery) {
  const notesElement = document.getElementById("notes");
  if (!notesElement) return baseQuery;
  
  const notesText = notesElement.value.toLowerCase().trim();
  if (!notesText) return baseQuery;

  // Regex looks for "avoid", "no", "without", "minus", "not" followed by a word
  const negativeMatches = notesText.match(/(?:avoid|no|without|minus|never|not)\s+([a-z]+)/g);
  let finalQuery = baseQuery;
  
  if (negativeMatches) {
    negativeMatches.forEach(match => {
      const parts = match.split(/\s+/);
      if (parts.length > 1) {
        finalQuery += ` -${parts[1]}`;
      }
    });
  }
  
  return finalQuery.trim();
}

async function fetchUnsplashImages(query) {
  exploreGrid.innerHTML = Array.from({ length: 6 }).map(() => '<div class="skeleton" style="height: 380px; border-radius: 1.8rem;"></div>').join("");
  
  const constrainedQuery = applyNegativeConstraints(query);

  try {
    const response = await fetch(`http://localhost:8080/api/unsplash?query=${encodeURIComponent(constrainedQuery)}&per_page=6&orientation=portrait`);
    if (!response.ok) throw new Error("Rate limit or auth issue with backend Unsplash proxy.");
    const data = await response.json();
    
    if (data.results && data.results.length > 0) {
      // Map Unsplash results perfectly into our Explore cards
      const unsplashCards = data.results.map((photo, index) => ({
        title: query.replace(" fashion outfit", "") + ` Vol. ${index + 1}`,
        description: photo.description || photo.alt_description || "High resolution fashion imagery fetched securely from Unsplash.",
        image: photo.urls.regular,
        audience: uiState.audience,
        style: uiState.style
      }));
      exploreGrid.innerHTML = unsplashCards.map(createExploreCardMarkup).join("");
    } else {
      throw new Error("No Unsplash results found.");
    }
  } catch (error) {
    console.error("Unsplash fetch failed, degrading to local fallbacks...", error);
    // Graceful degradation fallback using static mock data
    const visibleCards = exploreCards.filter(card => card.audience === uiState.audience && (uiState.style === "all" || card.style === uiState.style));
    const fallbackCards = visibleCards.length >= 3 ? visibleCards : exploreCards.filter(card => card.audience === uiState.audience).slice(0, 5);
    exploreGrid.innerHTML = fallbackCards.map(createExploreCardMarkup).join("");
  }
}

async function renderExploreGrid() {
  const audience = uiState.audience || "men";
  const style = uiState.style || "all";

  activeFilterTitle.textContent = `${capitalize(audience)} / ${style === "all" ? "All Looks" : formatLabel(style)}`;
  activeFilterDescription.textContent = buildFilterDescription(audience, style);

  // Auto-build the Unsplash live search query based on selected chips
  const query = `${capitalize(audience)} ${style === "all" ? "Fashion" : formatLabel(style)} Fashion Outfit`;
  
  fetchUnsplashImages(query);
}

function renderResultSet(resultSet) {
  const safeSet = ensureResultSet(resultSet);
  const activeOutfit = safeSet.outfits[uiState.activeOutfitIndex] || safeSet.outfits[0];

  outfitHeading.textContent = activeOutfit;
  resultStyle.textContent = `Style: ${formatLabel(safeSet.style)}`;
  activeAudienceChip.textContent = `Audience: ${formatLabel(safeSet.audience)}`;
  resultSource.textContent = getSourceLabel(safeSet.source);

  itemsList.innerHTML = safeSet.items.slice(0, 5).map((item) => {
    const key = buildItemKey(item);
    const link = safeSet.amazonLinks[key] || buildAmazonLink(item, safeSet.audience);

    return `
      <article class="item-card">
        <div>
          <p class="item-card-kicker">Detected piece</p>
          <h4 class="item-card-title">${escapeHtml(item)}</h4>
        </div>
        <a href="${escapeHtml(link)}" target="_blank" rel="noreferrer" class="shop-link">Buy Now</a>
      </article>
    `;
  }).join("");

  outfitCards.innerHTML = safeSet.outfits.map((outfit, index) => `
    <button type="button" class="outfit-option-card ${index === uiState.activeOutfitIndex ? "is-active" : ""}" data-outfit-index="${index}">
      <p class="outfit-option-kicker">Option ${index + 1}</p>
      <h4 class="outfit-option-title">${escapeHtml(outfit)}</h4>
    </button>
  `).join("");

  outfitCards.querySelectorAll("[data-outfit-index]").forEach((button) => {
    button.addEventListener("click", () => {
      uiState.activeOutfitIndex = Number(button.dataset.outfitIndex) || 0;
      renderResultSet(safeSet);
    });
  });

  previousOutfitButton.disabled = safeSet.outfits.length <= 1;
  nextOutfitButton.disabled = safeSet.outfits.length <= 1;

  // Dynamically sync Pinterest to the CURRENT active outfit block
  const pinterestQuery = activeOutfit || safeSet.pinterestQuery || buildPinterestQueryFromState(safeSet.style, safeSet.audience);
  const pinterestUrl = buildPinterestUrl(pinterestQuery);
  pinterestLink.href = pinterestUrl;
  pinterestHeading.textContent = formatLabel(pinterestQuery);
  pinterestDescription.textContent = `Pinterest inspiration tracking your active selection: ${formatLabel(pinterestQuery)}.`;
  loadPinterestImages(pinterestQuery);
}

async function loadPinterestImages(query) {
  if (!pinterestContainer) {
    return;
  }

  const normalizedQuery = String(query || buildPinterestQueryFromState("casual", uiState.audience)).replace(/\s+/g, " ").trim();
  const constrainedQuery = applyNegativeConstraints(normalizedQuery);
  
  pinterestContainer.innerHTML = Array.from({ length: 4 }).map(() => '<div class="skeleton" style="height: 380px; border-radius: 1.5rem;"></div>').join("");

  try {
    const response = await fetch(`http://localhost:8080/api/unsplash?query=${encodeURIComponent(constrainedQuery)}&per_page=4&orientation=portrait`);
    if (!response.ok) throw new Error("Backend Unsplash proxy limit reached.");
    const data = await response.json();

    if (data.results && data.results.length > 0) {
      pinterestContainer.innerHTML = "";
      data.results.forEach((photo, index) => {
        // As requested: "takes the exact title of the card and opens a Pinterest search for that specific outfit"
        const cardTitle = normalizedQuery;
        const pinterestUrl = `https://in.pinterest.com/search/pins/?q=${encodeURIComponent(cardTitle)}`;

        const card = document.createElement("div");
        card.className = "pinterest-card";
        card.tabIndex = 0;

        const img = document.createElement("img");
        img.src = photo.urls.regular;
        img.alt = photo.alt_description || cardTitle;
        img.className = "pinterest-card-image";
        img.loading = "lazy";

        const content = document.createElement("div");
        content.className = "pinterest-card-body";

        const title = document.createElement("h3");
        title.className = "pinterest-card-title";
        title.innerText = `Style Idea ${index + 1}`;

        const subtitle = document.createElement("p");
        subtitle.className = "pinterest-card-subtitle";
        subtitle.innerText = formatLabel(cardTitle);

        content.appendChild(title);
        content.appendChild(subtitle);

        card.appendChild(img);
        card.appendChild(content);

        card.onclick = () => {
          window.open(pinterestUrl, "_blank", "noopener,noreferrer");
        };

        card.onkeydown = (event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            window.open(pinterestUrl, "_blank", "noopener,noreferrer");
          }
        };

        pinterestContainer.appendChild(card);
      });
    } else {
      throw new Error("No Unsplash results found for Pinterest section.");
    }
  } catch (error) {
    console.error("Pinterest Unsplash fetch failed.", error);
    // Silent degradation handled by existing placeholder grids if needed, but we can just leave it as is or show an error
  }
}

function normalizeResultSet(data, formData) {
  const audience = String(data.audience || formData.get("audience") || uiState.audience || "men").toLowerCase();
  const style = String(data.style || formData.get("style") || "casual").toLowerCase();
  const items = sanitizeStringArray(data.items);
  const outfits = sanitizeStringArray(data.outfits);
  const derivedOutfits = outfits.length ? outfits : buildDefaultOutfits(style, audience);
  const derivedItems = items.length ? items : buildDefaultItems(style, audience);

  return {
    audience,
    style,
    items: ensureMinimumItems(derivedItems, style, audience),
    outfits: ensureMinimumOutfits(derivedOutfits, style, audience),
    amazonLinks: normalizeAmazonLinks(data.amazonLinks, derivedItems, audience),
    pinterestQuery: String(data.pinterestQuery || buildPinterestQueryFromState(style, audience)),
    source: String(data.source || "ai-image"),
  };
}

function deriveFilteredResultSet() {
  return ensureResultSet({
    ...(uiState.resultSet || buildDefaultResultSet()),
    audience: uiState.audience || "men",
    style: uiState.style === "all" ? (uiState.resultSet?.style || "casual") : uiState.style,
  });
}

function ensureResultSet(resultSet) {
  const base = resultSet || buildDefaultResultSet();
  const audience = String(base.audience || uiState.audience || "men").toLowerCase();
  const style = String(base.style || "casual").toLowerCase();
  const items = ensureMinimumItems(sanitizeStringArray(base.items), style, audience);
  const outfits = ensureMinimumOutfits(sanitizeStringArray(base.outfits), style, audience);

  if (uiState.activeOutfitIndex >= outfits.length) {
    uiState.activeOutfitIndex = 0;
  }

  return {
    ...base,
    audience,
    style,
    items,
    outfits,
    amazonLinks: normalizeAmazonLinks(base.amazonLinks, items, audience),
    pinterestQuery: String(base.pinterestQuery || buildPinterestQueryFromState(style, audience)),
  };
}

function buildDefaultResultSet() {
  const audience = uiState.audience || "men";
  const style = uiState.style === "all" ? "casual" : uiState.style;
  return {
    audience,
    style,
    items: buildDefaultItems(style, audience),
    outfits: buildDefaultOutfits(style, audience),
    amazonLinks: normalizeAmazonLinks({}, buildDefaultItems(style, audience), audience),
    pinterestQuery: buildPinterestQueryFromState(style, audience),
    source: "form-fallback",
  };
}

function buildDefaultItems(style, audience) {
  const catalog = {
    men: {
      casual: ["white shirt", "blue jeans", "clean sneakers", "lightweight overshirt", "minimal watch"],
      formal: ["oxford shirt", "tailored trousers", "derby shoes", "navy blazer", "leather belt"],
      genz: ["boxy tee", "cargo pants", "retro sneakers", "crossbody bag", "silver chain"],
      minimal: ["fine knit tee", "straight trousers", "leather sneakers", "overshirt", "clean watch"],
      athleisure: ["performance tee", "track pants", "running shoes", "zip jacket", "sport watch"],
    },
    women: {
      casual: ["neutral top", "blue jeans", "white sneakers", "cropped cardigan", "sleek tote"],
      formal: ["tailored blazer", "straight trousers", "pointed heels", "silk shell top", "structured tote"],
      genz: ["cropped graphic tee", "wide-leg jeans", "platform sneakers", "mini shoulder bag", "layered rings"],
      minimal: ["ribbed knit top", "cream trousers", "loafers", "soft blazer", "gold hoops"],
      athleisure: ["performance tee", "leggings", "running shoes", "light jacket", "sport tote"],
    },
    kids: {
      casual: ["graphic sweatshirt", "soft denim", "play sneakers", "light cap", "mini backpack"],
      formal: ["neat shirt", "tailored chinos", "polished sneakers", "soft cardigan", "dress watch"],
      genz: ["hoodie", "joggers", "chunky trainers", "crossbody pouch", "beanie"],
      minimal: ["solid tee", "easy trousers", "slip-on shoes", "overshirt", "small backpack"],
      athleisure: ["sport tee", "track pants", "running shoes", "zip hoodie", "duffle bag"],
    },
  };

  return catalog[audience]?.[style] || catalog[audience]?.casual || catalog.men.casual;
}

function buildDefaultOutfits(style, audience) {
  const lookbook = {
    men: {
      casual: ["white shirt + blue jeans + sneakers", "black t-shirt + cargo pants + sneakers", "hoodie + joggers + trainers"],
      formal: ["oxford shirt + tailored trousers + derby shoes", "fine knit polo + pleated pants + loafers", "navy blazer + chinos + leather sneakers"],
      genz: ["boxy tee + baggy jeans + chunky sneakers", "graphic hoodie + cargo pants + retro trainers", "layered tee + relaxed denim + skate shoes"],
      minimal: ["fine knit tee + straight trousers + leather sneakers", "overshirt + tapered denim + loafers", "cream shirt + black trousers + court sneakers"],
      athleisure: ["performance jacket + track pants + running shoes", "dry-fit tee + joggers + knit trainers", "zip hoodie + tech pants + lifestyle sneakers"],
    },
    women: {
      casual: ["neutral top + blue jeans + white sneakers", "cropped cardigan + midi skirt + sleek flats", "ribbed tank + relaxed denim + loafers"],
      formal: ["tailored blazer + straight trousers + pointed heels", "silk blouse + tapered pants + slingback pumps", "belted co-ord set + loafers + structured tote"],
      genz: ["cropped graphic tee + wide-leg jeans + platform sneakers", "baby tee + parachute pants + chunky sneakers", "oversized shirt + mini skirt + high-top sneakers"],
      minimal: ["ribbed knit top + cream trousers + loafers", "soft blazer + knit dress + ankle boots", "monochrome co-ord + sleek sneakers + tote"],
      athleisure: ["performance tee + leggings + trainers", "zip jacket + flare pants + sneakers", "sport bra + cargo joggers + running shoes"],
    },
    kids: {
      casual: ["graphic sweatshirt + soft denim + play sneakers", "striped tee + cargo shorts + sporty sandals", "hoodie + joggers + colorful trainers"],
      formal: ["neat shirt + chinos + polished sneakers", "soft cardigan + trousers + loafers", "mini blazer + denim + slip-ons"],
      genz: ["oversized hoodie + joggers + chunky trainers", "graphic tee + cargo pants + bright sneakers", "check shirt + relaxed jeans + skate shoes"],
      minimal: ["solid tee + easy trousers + slip-on shoes", "soft knit + denim + clean sneakers", "overshirt + joggers + low-top trainers"],
      athleisure: ["sport tee + track pants + running shoes", "zip hoodie + shorts + trainers", "performance top + joggers + sporty sandals"],
    },
  };

  return lookbook[audience]?.[style] || lookbook[audience]?.casual || lookbook.men.casual;
}

function ensureMinimumItems(items, style, audience) {
  const merged = [...new Set([...items, ...buildDefaultItems(style, audience)])];
  return merged.slice(0, 5);
}

function ensureMinimumOutfits(outfits, style, audience) {
  const merged = [...new Set([...outfits, ...buildDefaultOutfits(style, audience)])];
  return merged.slice(0, 5);
}

function normalizeAmazonLinks(links, items, audience) {
  const normalized = {};
  items.forEach((item) => {
    const key = buildItemKey(item);
    normalized[key] = links?.[key] || buildAmazonLink(item, audience);
  });
  return normalized;
}

function sanitizeStringArray(value) {
  return Array.isArray(value)
    ? value.map((entry) => String(entry || "").trim()).filter(Boolean)
    : [];
}

function updateFilterButtons(group, value) {
  document.querySelectorAll(`[data-filter-group="${group}"]`).forEach((button) => {
    button.classList.toggle("is-active", button.dataset.filterValue === value);
  });
}

function createExploreCardMarkup(card) {
  return `
    <article class="product-card" data-category="${escapeHtml(card.audience)}" data-style="${escapeHtml(card.style)}">
      <img src="${escapeHtml(card.image)}" alt="${escapeHtml(card.title)}" class="product-image" />
      <div class="product-body">
        <div class="product-header">
          <p class="product-kicker">${escapeHtml(capitalize(card.audience))}</p>
          <span class="product-pill">${escapeHtml(formatLabel(card.style))}</span>
        </div>
        <h3 class="product-title">${escapeHtml(card.title)}</h3>
        <p class="product-copy">${escapeHtml(card.description)}</p>
      </div>
    </article>
  `;
}

function getSourceLabel(source) {
  const labels = {
    "ai-image": "Image analyzed with AI vision for outfit-specific suggestions.",
    "fallback-after-image": "Image upload was received, but a safe fallback outfit set was generated from your inputs.",
    "form-fallback": "No image was required. This outfit set was generated from your selected style, purpose, and audience.",
  };
  return labels[source] || labels["form-fallback"];
}

function buildFilterDescription(audience, style) {
  if (style === "all") {
    return `A full set of ${audience} styling references across casual, minimal, formal, and trend-led looks.`;
  }
  return `${capitalize(audience)} looks focused on ${formatLabel(style).toLowerCase()} styling with multiple outfit options.`;
}

function buildPinterestUrl(query) {
  return `https://in.pinterest.com/search/pins/?q=${encodeURIComponent(query)}`;
}

function buildPinterestQueryFromState(style, audience) {
  return `${style} ${audience} outfit`.replace(/\s+/g, " ").trim();
}

function buildAmazonLink(item, audience) {
  return `https://www.amazon.in/s?k=${encodeURIComponent(`${item} ${audience}`)}`;
}

function buildItemKey(item) {
  const words = String(item).toLowerCase().trim().split(/\s+/);
  return words[words.length - 1]?.replace(/[^a-z]/g, "") || "item";
}

function setLoadingState(isLoading) {
  submitButton.disabled = isLoading;
  if (rethinkOutfitButton) {
    rethinkOutfitButton.disabled = isLoading;
  }
  loadingSpinner.classList.toggle("hidden", !isLoading);
  buttonLabel.textContent = isLoading ? "Styling..." : "Get Outfit 🔥";
}

function setStatus(message, state) {
  formStatus.textContent = message;
  formStatus.classList.remove("status-success", "status-error");
  if (state === "success") {
    formStatus.classList.add("status-success");
  }
  if (state === "error") {
    formStatus.classList.add("status-error");
  }
}

function formatFileSize(size) {
  if (!Number.isFinite(size) || size <= 0) {
    return "0 KB";
  }
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }
  return `${Math.max(1, Math.round(size / 1024))} KB`;
}

function formatLabel(value) {
  return String(value)
    .replace(/[_-]+/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase())
    .trim();
}

function capitalize(value) {
  return String(value).charAt(0).toUpperCase() + String(value).slice(1);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
