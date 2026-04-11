import re

html_path = 'frontend/index.html'
with open(html_path, 'r') as f:
    html = f.read()

# Replace Tailwind configs
html = html.replace('ink: "#111111"', 'ink: "#ffffff"')
html = html.replace('stone: "#f3f0ea"', 'stone: "#1a1a1a"')
html = html.replace('sand: "#e9e1d2"', 'sand: "#242424"')
html = html.replace('luxe: "0 25px 70px rgba(17, 17, 17, 0.12)"', 'luxe: "0 25px 70px rgba(0, 0, 0, 0.5)"')

# Global background
html = html.replace('bg-[#f8f5ef]', 'bg-[#050505]')

# Text replacements
html = html.replace('text-black', 'text-white')
html = re.sub(r'border-black/(\d+)', r'border-white/\1', html)
html = re.sub(r'text-black/(\d+)', r'text-white/\1', html)
html = html.replace('bg-white/70', 'bg-white/10')
html = html.replace('bg-white/75', 'bg-white/5')
html = html.replace('bg-white/80', 'bg-white/10')
html = html.replace('bg-white/60', 'bg-white/5')
html = html.replace('bg-[#f5efe4]', 'bg-white/5')
html = html.replace('bg-[#f1ebe0]', 'bg-white/5')
html = html.replace('bg-[#f5f1ea]', 'bg-white/10')
html = html.replace('border-black/15', 'border-white/15')

# Ensure glassmorphism backdrop blurs
html = html.replace('bg-white/5 p-6 shadow-luxe', 'bg-white/5 p-6 shadow-luxe backdrop-blur-xl')
html = html.replace('bg-white/10 p-6 shadow-luxe', 'bg-white/10 p-6 shadow-luxe backdrop-blur-xl')
html = html.replace('bg-white/75 p-5 shadow-sm', 'bg-white/5 p-5 shadow-sm backdrop-blur-lg border border-white/10')
html = html.replace('bg-white p-4 shadow-lg', 'bg-white/10 p-4 shadow-lg backdrop-blur-lg border border-white/10')

# Specifically fix grid containers for exploreGrid and outfitCards (remove tailwind grid classes as they will go to css)
html = html.replace('class="grid gap-6 sm:grid-cols-2 xl:grid-cols-3"', '')
html = html.replace('class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3"', '')
# Wait, replacing empty class is bad, let's just replace the exact tags
html = html.replace('<div id="exploreGrid" class="grid gap-6 sm:grid-cols-2 xl:grid-cols-3"></div>', '<div id="exploreGrid"></div>')
html = html.replace('<div id="outfitCards" class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3"></div>', '<div id="outfitCards"></div>')

# Fix buttons
html = html.replace('bg-black text-white py-3', 'bg-white text-black py-3')
html = html.replace('hover:bg-gray-800', 'hover:bg-gray-200')

with open(html_path, 'w') as f:
    f.write(html)
print("Finished modifying HTML.")
