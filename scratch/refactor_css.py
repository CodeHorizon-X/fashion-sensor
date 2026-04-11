import re

css_path = 'frontend/styles.css'
with open(css_path, 'r') as f:
    css = f.read()

# 1. Body background
css = css.replace('radial-gradient(circle at top left, rgba(255, 255, 255, 0.9), transparent 28%),\n    linear-gradient(180deg, #faf7f1 0%, #f5efe4 46%, #f8f5ef 100%)', 'radial-gradient(circle at top left, rgba(255, 255, 255, 0.1), transparent 28%),\n    linear-gradient(180deg, #0f0f0f 0%, #050505 46%, #000000 100%)')

# 2. General Colors Replacement
# Convert black shadows to lighter or keep them dark for dark theme? Dark theme usually has dark shadows (black). So rgba(17,17,17) -> black or keep it.
# Text #111111 -> #ffffff
css = css.replace('#111111', '#ffffff')
# Backgrounds: 255, 255, 255 -> 255, 255, 255, 0.05 etc.
# Nav-tab active
css = css.replace('background: #111111;', 'background: #ffffff;') # Wait, #111 -> #fff happened above. Let's fix that.

# Let's read from scratch and parse properly
css = css.replace('background: #ffffff;\n  color: #ffffff;\n  border-color: #ffffff;', 'background: #ffffff;\n  color: #000000;\n  border-color: #ffffff;')

# Let's just fix it procedurally:
