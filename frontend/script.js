const form = document.getElementById("fashionForm");
const photoInput = document.getElementById("photo");
const previewContainer = document.getElementById("previewContainer");
const imagePreview = document.getElementById("imagePreview");
const fileMeta = document.getElementById("fileMeta");
const submitButton = document.getElementById("submitButton");
const loadingSpinner = document.getElementById("loadingSpinner");
const buttonLabel = submitButton?.querySelector(".button-label");
const formStatus = document.getElementById("formStatus");
const suggestedGrid = document.getElementById("suggestedGrid");

const API_URL = "http://localhost:8080/api/suggest";
let previewUrl = null;

const placeholderProducts = [
  {
    image: "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=900&q=80",
    itemType: "Top",
    name: "Oversized White Tee",
    style: "Minimal",
    description: "Soft tailoring and elevated basics for clean everyday dressing.",
    tags: ["Editorial", "Minimal", "Everyday"],
  },
  {
    image: "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80",
    itemType: "Bottom",
    name: "Black Cargo Pants",
    style: "Gen Z",
    description: "Relaxed silhouettes with statement proportions and confident casual energy.",
    tags: ["Street", "Weekend", "Layered"],
  },
  {
    image: "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=900&q=80",
    itemType: "Shoes",
    name: "Chunky Sneakers",
    style: "Formal",
    description: "Structured separates that balance polished tailoring with comfort.",
    tags: ["Formal", "Office", "Sharp"],
  },
  {
    image: "https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=900&q=80",
    itemType: "Accessories",
    name: "Leather Crossbody",
    style: "Party",
    description: "Textured layers and richer tones built for dinner dates and evenings out.",
    tags: ["Night", "Dressy", "Textures"],
  },
];

renderSuggestedGrid(placeholderProducts);

photoInput?.addEventListener("change", handleImagePreview);
form?.addEventListener("submit", handleSubmit);

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
  setLoadingState(true);
  setStatus("Generating outfit recommendations...", "success");

  console.log("Submitting outfit request to:", API_URL);
  console.log("Form fields:", {
    date: formData.get("date"),
    location: formData.get("location"),
    withWhom: formData.get("withWhom"),
    purpose: formData.get("purpose"),
    style: formData.get("style"),
    notes: formData.get("notes"),
    photoAttached: Boolean(formData.get("photo") && formData.get("photo").name),
  });

  try {
    const response = await fetch(API_URL, {
      method: "POST",
      body: formData,
    });

    console.log("Response status:", response.status, response.statusText);

    const json = await response.json();
    console.log("Response JSON:", json);

    if (!response.ok) {
      throw new Error(json.error || `Server error (${response.status})`);
    }

    const generatedProducts = buildProductsFromResponse(json);
    renderSuggestedGrid(generatedProducts);
    setStatus("Recommendations updated.", "success");
    document.getElementById("results")?.scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (error) {
    console.error("Failed to fetch /api/suggest:", error);
    renderSuggestedGrid(
      placeholderProducts,
      "We could not update the AI outfit grid right now, so the editorial placeholders are still shown."
    );
    setStatus(error.message || "Something went wrong while contacting the server.", "error");
  } finally {
    setLoadingState(false);
  }
}

function buildProductsFromResponse(data) {
  const products = [
    createDetailProduct("Top", pickValue(data.top, data.shirt, data.upperwear), data.style, data),
    createDetailProduct("Bottom", pickValue(data.bottom, data.pants, data.trousers), data.style, data),
    createDetailProduct("Shoes", pickValue(data.footwear, data.shoes), data.style, data),
    createDetailProduct("Accessories", pickValue(data.accessories, data.accessory, data.notes, data.tip), data.style, data),
  ].filter(Boolean);

  if (!products.length) {
    products.push({
      itemType: "AI Outfit",
      image: getPlaceholderImage("AI Outfit"),
      name: deriveStylishName(
        pickValue(data.summary, data.title, data.recommendation, `${formatLabel(data.purpose || "Occasion")} Outfit`)
      ),
      style: pickValue(data.style, data.occasion, "AI Edit"),
      description: pickValue(
        data.description,
        data.outfit,
        "A personalized recommendation tailored to your styling brief."
      ),
      tags: [pickValue(data.purpose, data.occasion), data.style, pickValue(data.location)].filter(Boolean),
    });
  }

  while (products.length < 4) {
    products.push(placeholderProducts[products.length]);
  }

  return products.slice(0, 4);
}

function createDetailProduct(itemType, rawValue, style, data) {
  if (!rawValue) {
    return null;
  }

  return {
    itemType,
    image: getPlaceholderImage(itemType),
    name: deriveStylishName(rawValue),
    style: pickValue(style, "Styled"),
    description: formatLabel(rawValue),
    tags: [pickValue(data.purpose, data.occasion), style, "AI Pick"].filter(Boolean),
  };
}

function renderSuggestedGrid(products, helperText = "") {
  suggestedGrid.innerHTML = products.map(createProductCardMarkup).join("");

  if (helperText) {
    const note = document.createElement("p");
    note.className = "col-span-full mt-2 text-sm text-black/55";
    note.textContent = helperText;
    suggestedGrid.append(note);
  }
}

function createProductCardMarkup(product) {
  const tags = (product.tags || []).filter(Boolean).slice(0, 3);

  return `
    <article class="product-card">
      <img src="${escapeHtml(product.image)}" alt="${escapeHtml(product.name)}" class="product-image" />
      <div class="product-body">
        <div class="product-header">
          <p class="product-kicker">${escapeHtml(formatLabel(product.itemType || "AI Look"))}</p>
          <span class="product-pill">${escapeHtml(formatLabel(product.style || "AI Look"))}</span>
        </div>
        <h3 class="product-title">${escapeHtml(formatLabel(product.name))}</h3>
        <p class="product-copy">${escapeHtml(formatLabel(product.description || ""))}</p>
        <div class="product-meta">
          ${tags.map((tag) => `<span>${escapeHtml(formatLabel(tag))}</span>`).join("")}
        </div>
      </div>
    </article>
  `;
}

function setLoadingState(isLoading) {
  submitButton.disabled = isLoading;
  loadingSpinner.classList.toggle("hidden", !isLoading);
  buttonLabel.textContent = isLoading ? "Styling..." : "Get Outfit";
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

function pickValue(...values) {
  return values.find((value) => typeof value === "string" && value.trim()) || "";
}

function formatLabel(value) {
  return String(value).replace(/[_-]+/g, " ").trim();
}

function deriveStylishName(rawValue) {
  const cleaned = formatLabel(rawValue)
    .replace(/\b(top|bottom|shoes|accessories|pairing|finish|recommendation)\b/gi, "")
    .replace(/\s+/g, " ")
    .trim();

  if (!cleaned) {
    return "Curated Fashion Pick";
  }

  return cleaned
    .split(" ")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function getPlaceholderImage(itemType) {
  const imageMap = {
    Top: "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80",
    Bottom: "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=900&q=80",
    Shoes: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80",
    Accessories: "https://images.unsplash.com/photo-1523170335258-f5ed11844a49?auto=format&fit=crop&w=900&q=80",
    "AI Outfit": "https://images.unsplash.com/photo-1506629905607-fcf205f90bba?auto=format&fit=crop&w=900&q=80",
  };

  return imageMap[itemType] || imageMap["AI Outfit"];
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
