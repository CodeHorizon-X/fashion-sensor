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

const API_URL = "http://localhost:8080/api/suggest";
const SUGGESTIONS_URL = "http://localhost:8080/api/suggestions";
let previewUrl = null;

const uiState = {
  section: "home",
  audience: audienceSelect?.value || "men",
  style: "all",
  resultSet: null,
  activeOutfitIndex: 0,
  formPayload: null,
  refreshRequestId: 0,
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
  void fetchExploreData(uiState.audience, uiState.style === "all" ? "casual" : uiState.style);
  renderResultSet(buildDefaultResultSet());
  bindSectionNavigation();
  bindFilters();
  bindFormSync();
  bindResultControls();

  photoInput?.addEventListener("change", handleImagePreview);
  form?.addEventListener("submit", handleSubmit);
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

      // Immediately refresh exploreGrid with live Unsplash images from the backend
      const audience = uiState.audience || "men";
      const style = uiState.style === "all" ? "casual" : uiState.style;
      void fetchExploreData(audience, style);

      renderResultSet(deriveFilteredResultSet());
      loadPinterestImages(buildPinterestQueryFromState(style, audience));
      void refreshResultsForCurrentFilters();
    });
  });
}

/**
 * Fetches live Unsplash images from GET /api/suggestions?audience=&style=
 * and re-renders the exploreGrid. Falls back to the local static data on error.
 */
async function fetchExploreData(audience, style) {
  // Update the label immediately so the UI feels responsive
  activeFilterTitle.textContent = `${capitalize(audience)} / ${style === "all" ? "All Looks" : formatLabel(style)}`;
  activeFilterDescription.textContent = buildFilterDescription(audience, style);

  try {
    const url = `${SUGGESTIONS_URL}?audience=${encodeURIComponent(audience)}&style=${encodeURIComponent(style)}`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Server error ${response.status}`);
    }
    const data = await response.json();
    const items = Array.isArray(data.items) ? data.items : [];

    if (items.length === 0) {
      // Backend returned nothing useful — fall through to static fallback
      renderExploreGrid();
      return;
    }

    // Render live Unsplash cards; each Pinterest link points to a search for item.title
    exploreGrid.innerHTML = items.map((item) => {
      const title  = escapeHtml(item.title  || "Fashion Look");
      const imgSrc = escapeHtml(item.imageUrl || "");
      const pinUrl = escapeHtml(buildPinterestUrl(item.title || `${audience} ${style} fashion outfit`));
      return `
        <a href="${pinUrl}" target="_blank" rel="noreferrer" class="product-card block no-underline" aria-label="View ${title} on Pinterest">
          <img src="${imgSrc}" alt="${title}" class="product-image" loading="lazy"
               onerror="this.onerror=null;this.src='https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80'" />
          <div class="product-body">
            <div class="product-header">
              <p class="product-kicker">${escapeHtml(capitalize(audience))}</p>
              <span class="product-pill">${escapeHtml(formatLabel(style))}</span>
            </div>
            <h3 class="product-title">${title}</h3>
            <p class="product-copy">Click to explore on Pinterest →</p>
          </div>
        </a>
      `;
    }).join("");
  } catch (error) {
    console.warn("fetchExploreData failed, using static fallback.", error);
    renderExploreGrid(); // static fallback
  }
}

function bindFormSync() {
  audienceSelect?.addEventListener("change", () => {
    uiState.audience = audienceSelect.value;
    updateFilterButtons("audience", uiState.audience);
    const style = uiState.style === "all" ? "casual" : uiState.style;
    void fetchExploreData(uiState.audience, style);
    renderResultSet(deriveFilteredResultSet());
    loadPinterestImages(buildPinterestQueryFromState(style, uiState.audience));
    void refreshResultsForCurrentFilters();
  });

  styleSelect?.addEventListener("change", () => {
    uiState.style = styleSelect.value;
    updateFilterButtons("style", uiState.style);
    const style = uiState.style === "all" ? "casual" : uiState.style;
    void fetchExploreData(uiState.audience, style);
    renderResultSet(deriveFilteredResultSet());
    loadPinterestImages(buildPinterestQueryFromState(style, uiState.audience));
    void refreshResultsForCurrentFilters();
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

  setLoadingState(true);
  setStatus("Creating your AI styling board...", "success");
  setActiveSection("results");

  try {
    await fetchAndRenderLatestResults({ resetOutfitIndex: true });
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
      await fetchAndRenderLatestResults({ resetOutfitIndex: true });
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

async function refreshResultsForCurrentFilters() {
  try {
    await fetchAndRenderLatestResults({ resetOutfitIndex: true, backgroundRefresh: true });
  } catch (error) {
    console.warn("Unable to refresh filtered results.", error);
  }
}

async function fetchAndRenderLatestResults({ resetOutfitIndex = false, backgroundRefresh = false } = {}) {
  const requestId = ++uiState.refreshRequestId;
  const formData = buildRequestFormData();

  uiState.audience = String(formData.get("audience") || uiState.audience || "men");
  uiState.style = String(formData.get("style") || uiState.style || "casual");
  uiState.formPayload = new FormData(formData);

  if (!backgroundRefresh) {
    setLoadingState(true);
  }

  try {
    const response = await fetch(API_URL, {
      method: "POST",
      body: formData,
    });

    const json = await response.json();
    if (!response.ok) {
      throw new Error(json.error || `Server error (${response.status})`);
    }

    if (requestId !== uiState.refreshRequestId) {
      return;
    }

    uiState.resultSet = normalizeResultSet(json, formData);
    if (resetOutfitIndex) {
      uiState.activeOutfitIndex = 0;
    }
    renderResultSet(deriveFilteredResultSet());
    loadPinterestImages(uiState.resultSet.pinterestQuery);
  } finally {
    if (!backgroundRefresh) {
      setLoadingState(false);
    }
  }
}

function buildRequestFormData() {
  const formData = form ? new FormData(form) : new FormData();
  const backendStyle = resolveBackendStyleValue();

  formData.set("audience", uiState.audience || audienceSelect?.value || "men");
  formData.set("style", backendStyle);

  if (audienceSelect) {
    audienceSelect.value = String(formData.get("audience"));
  }

  if (styleSelect) {
    styleSelect.value = backendStyle;
  }

  return formData;
}

function resolveBackendStyleValue() {
  const selectedStyle = uiState.style || styleSelect?.value || "casual";
  if (selectedStyle === "all") {
    return styleSelect?.value || "casual";
  }
  return selectedStyle;
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

function setActiveSection(sectionId) {
  uiState.section = sectionId;

  document.querySelectorAll("[data-section-target]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.sectionTarget === sectionId);
  });

  document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function renderExploreGrid() {
  const audience = uiState.audience || "men";
  const style = uiState.style || "all";

  const visibleCards = exploreCards.filter((card) => {
    const audienceMatch = card.audience === audience;
    const styleMatch = style === "all" || card.style === style;
    return audienceMatch && styleMatch;
  });

  const cardsToRender = visibleCards.length >= 3 ? visibleCards : exploreCards.filter((card) => card.audience === audience).slice(0, 5);

  activeFilterTitle.textContent = `${capitalize(audience)} / ${style === "all" ? "All Looks" : formatLabel(style)}`;
  activeFilterDescription.textContent = buildFilterDescription(audience, style);
  exploreGrid.innerHTML = cardsToRender.map(createExploreCardMarkup).join("");
}

function renderResultSet(resultSet) {
  const safeSet = ensureResultSet(resultSet);
  const activeOutfit = safeSet.outfits[uiState.activeOutfitIndex] || safeSet.outfits[0];

  outfitHeading.textContent = activeOutfit;
  resultStyle.textContent = `Style: ${formatLabel(safeSet.style)}`;
  activeAudienceChip.textContent = `Audience: ${formatLabel(safeSet.audience)}`;
  resultSource.textContent = getSourceLabel(safeSet.source);

  itemsList.innerHTML = safeSet.itemCards.slice(0, 5).map((item) => {
    const key = buildItemKey(item.title);
    const shopLink = safeSet.amazonLinks[key] || buildAmazonLink(item.title, safeSet.audience);
    const pinterestItemUrl = buildPinterestUrl(item.title);
    const backgroundImage = escapeAttribute(item.imageUrl || buildFallbackImageUrl(item.title, safeSet.style, safeSet.audience));
    const safeTitle = escapeHtml(item.title);

    return `
      <article class="item-card">
        <div class="item-card-media" data-item-title="${safeTitle}" style="background-image: url('${backgroundImage}');"></div>
        <div class="item-card-content">
          <div>
            <p class="item-card-kicker">Outfit component</p>
            <h4 class="item-card-title">${safeTitle}</h4>
          </div>
          <div class="item-card-actions">
            <a href="${escapeHtml(pinterestItemUrl)}" target="_blank" rel="noreferrer"
               class="shop-link" aria-label="View Pinterest ideas for ${safeTitle}">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="currentColor" aria-hidden="true"><path d="M12 0C5.373 0 0 5.373 0 12c0 5.084 3.163 9.426 7.627 11.174-.105-.949-.2-2.405.042-3.441.218-.937 1.407-5.965 1.407-5.965s-.359-.719-.359-1.782c0-1.668.967-2.914 2.171-2.914 1.023 0 1.518.769 1.518 1.69 0 1.029-.655 2.568-.994 3.995-.283 1.194.599 2.169 1.777 2.169 2.133 0 3.772-2.249 3.772-5.495 0-2.873-2.064-4.882-5.012-4.882-3.414 0-5.418 2.561-5.418 5.207 0 1.031.397 2.138.893 2.738a.36.36 0 0 1 .083.345l-.333 1.36c-.053.22-.174.267-.402.161-1.499-.698-2.436-2.889-2.436-4.649 0-3.785 2.75-7.262 7.929-7.262 4.163 0 7.398 2.967 7.398 6.931 0 4.136-2.607 7.464-6.227 7.464-1.216 0-2.359-.632-2.75-1.378l-.748 2.853c-.271 1.043-1.002 2.35-1.492 3.146C9.57 23.812 10.763 24 12 24c6.627 0 12-5.373 12-12S18.627 0 12 0Z"/></svg>
              View Pin Ideas
            </a>
            <a href="${escapeHtml(shopLink)}" target="_blank" rel="noreferrer"
               class="item-card-shop" aria-label="Search Amazon for ${safeTitle}">
              Amazon
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
            </a>
          </div>
        </div>
      </article>
    `;
  }).join("");

  // Asynchronously re-paint each card with a real per-item Unsplash image
  void enrichItemImages(safeSet.itemCards, safeSet.style, safeSet.audience);

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

  const pinterestQuery = safeSet.pinterestQuery || buildPinterestQueryFromState(safeSet.style, safeSet.audience);
  const pinterestUrl = buildPinterestUrl(pinterestQuery);
  pinterestLink.href = pinterestUrl;
  pinterestHeading.textContent = formatLabel(pinterestQuery);
  pinterestDescription.textContent = `Pinterest inspiration now tracks ${formatLabel(safeSet.audience)} looks with ${formatLabel(safeSet.style).toLowerCase()} styling references.`;
  loadPinterestImages(pinterestQuery);
}

/**
 * Calls GET /api/suggestions to get per-item Unsplash images,
 * then patches the background-image of every rendered .item-card-media element.
 */
async function enrichItemImages(itemCards, style, audience) {
  try {
    const effectiveStyle = (!style || style === "all") ? "casual" : style;
    const url = `${SUGGESTIONS_URL}?audience=${encodeURIComponent(audience)}&style=${encodeURIComponent(effectiveStyle)}`;
    const response = await fetch(url);
    if (!response.ok) return;

    const data = await response.json();
    const backendItems = Array.isArray(data.items) ? data.items : [];

    // Build a normalised title → imageUrl lookup
    const imageMap = {};
    backendItems.forEach((item) => {
      if (item.title && item.imageUrl) {
        imageMap[item.title.toLowerCase().trim()] = item.imageUrl;
      }
    });

    // Also honour imageUrls already on the itemCards (from a prior AI-image call)
    itemCards.forEach((card) => {
      const isGeneric = !card.imageUrl || card.imageUrl.includes("photo-1445205170230");
      if (card.title && !isGeneric) {
        imageMap[card.title.toLowerCase().trim()] = card.imageUrl;
      }
    });

    // Patch the DOM — only update elements currently in the shortlist
    document.querySelectorAll(".item-card-media[data-item-title]").forEach((el) => {
      const titleKey = el.dataset.itemTitle?.toLowerCase().trim() || "";
      if (titleKey && imageMap[titleKey]) {
        el.style.backgroundImage = `url('${escapeAttribute(imageMap[titleKey])}')`;
      }
    });
  } catch (err) {
    console.warn("enrichItemImages: could not load per-item images.", err);
  }
}

function loadPinterestImages(query) {
  if (!pinterestContainer) {
    return;
  }

  const normalizedQuery = String(query || buildPinterestQueryFromState("casual", uiState.audience))
    .replace(/\s+/g, " ")
    .trim();
  const descriptors = pinterestDescriptors[uiState.audience] || pinterestDescriptors.men;
  const pinterestUrl = buildPinterestUrl(normalizedQuery);

  pinterestContainer.innerHTML = "";

  for (let index = 0; index < 4; index += 1) {
    const descriptor = descriptors[index % descriptors.length];
    const imageQuery = `${normalizedQuery} ${descriptor}`;

    const card = document.createElement("div");
    card.className = "pinterest-card";
    card.tabIndex = 0;

    const img = document.createElement("img");
    img.src = `https://source.unsplash.com/400x500/?${encodeURIComponent(imageQuery)},fashion&sig=${index + Date.now()}`;
    img.alt = imageQuery;
    img.className = "pinterest-card-image";
    img.loading = "lazy";
    img.onerror = () => {
      img.onerror = null;
      img.src = "https://via.placeholder.com/400x500?text=Style+Inspiration";
    };

    const content = document.createElement("div");
    content.className = "pinterest-card-body";

    const title = document.createElement("h3");
    title.className = "pinterest-card-title";
    title.innerText = `Style Idea ${index + 1}`;

    const subtitle = document.createElement("p");
    subtitle.className = "pinterest-card-subtitle";
    subtitle.innerText = formatLabel(imageQuery);

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
  }
}

function normalizeResultSet(data, formData) {
  const audience = String(data.audience || formData.get("audience") || uiState.audience || "men").toLowerCase();
  const style = String(data.style || formData.get("style") || "casual").toLowerCase();
  const items = sanitizeStringArray(data.items);
  const outfits = sanitizeStringArray(data.outfits);
  const derivedOutfits = outfits.length ? outfits : buildDefaultOutfits(style, audience);
  const derivedItems = items.length ? items : buildDefaultItems(style, audience);
  const itemCards = normalizeItemCards(data.itemCards, derivedItems, style, audience);

  return {
    audience,
    style,
    items: ensureMinimumItems(derivedItems, style, audience),
    itemCards,
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
  const itemCards = normalizeItemCards(base.itemCards, items, style, audience);

  if (uiState.activeOutfitIndex >= outfits.length) {
    uiState.activeOutfitIndex = 0;
  }

  return {
    ...base,
    audience,
    style,
    items,
    itemCards,
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
    itemCards: buildDefaultItemCards(style, audience),
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

function normalizeItemCards(value, items, style, audience) {
  const normalized = Array.isArray(value)
    ? value
      .map((entry) => {
        if (!entry) {
          return null;
        }

        if (typeof entry === "string") {
          return {
            title: entry.trim(),
            imageUrl: buildFallbackImageUrl(entry, style, audience),
          };
        }

        const title = String(entry.title || entry.name || "").trim();
        if (!title) {
          return null;
        }

        return {
          title,
          imageUrl: String(entry.imageUrl || buildFallbackImageUrl(title, style, audience)).trim(),
        };
      })
      .filter(Boolean)
    : [];

  const merged = [...normalized];
  ensureMinimumItems(items, style, audience).forEach((item) => {
    if (!merged.some((entry) => entry.title.toLowerCase() === item.toLowerCase())) {
      merged.push({
        title: item,
        imageUrl: buildFallbackImageUrl(item, style, audience),
      });
    }
  });

  return merged.slice(0, 5);
}

function buildDefaultItemCards(style, audience) {
  return buildDefaultItems(style, audience).map((item) => ({
    title: item,
    imageUrl: buildFallbackImageUrl(item, style, audience),
  }));
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
  return `https://www.pinterest.com/search/pins/?q=${encodeURIComponent(query)}`;
}

function buildPinterestQueryFromState(style, audience) {
  return `${style} ${audience} outfit`.replace(/\s+/g, " ").trim();
}

function buildAmazonLink(item, audience) {
  // Spec: audience first, then item name; open on amazon.com
  return `https://www.amazon.com/s?k=${encodeURIComponent(`${audience} ${item}`)}`;
}

/**
 * Returns a varied Unsplash image URL for each item using a curated seed map.
 * This eliminates the single-photo fallback before the async enrichment runs.
 */
function buildFallbackImageUrl(item, style, audience) {
  // Curated Unsplash photo IDs grouped by style keyword
  const seedsByStyle = {
    formal:     ["1507679799987", "1598032895455", "1490367532201", "1541532642820"],
    minimal:    ["1483985988355", "1512436991641", "1434389677669", "1489987707849"],
    genz:       ["1529139574466", "1515886657613", "1524504388940", "1503919545889"],
    athleisure: ["1571019613454", "1571731956672", "1556742031526", "1544216717564"],
    casual:     ["1515886657613", "1496747611176", "1519238263530", "1516627145497"],
  };
  const seeds = seedsByStyle[style] || seedsByStyle.casual;
  // Derive a stable index from the item title so same item always picks same photo
  const idx = Math.abs(
    [...String(item)].reduce((acc, ch) => acc + ch.charCodeAt(0), 0)
  ) % seeds.length;
  const photoId = seeds[idx];
  return `https://images.unsplash.com/photo-${photoId}?auto=format&fit=crop&w=900&q=80`;
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

function escapeAttribute(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("'", "\\'");
}
