from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).parent / "previews"
groups = {}
for image_path in root.glob("*.png"):
    book, sheet = image_path.stem.split("__", 1)
    groups.setdefault(book, []).append((sheet, image_path))

font = ImageFont.load_default()
for book, items in groups.items():
    items.sort()
    tile_w, tile_h = 520, 300
    cols = 2
    rows = (len(items) + cols - 1) // cols
    canvas = Image.new("RGB", (cols * tile_w, rows * tile_h), "white")
    draw = ImageDraw.Draw(canvas)
    for i, (sheet, image_path) in enumerate(items):
        img = Image.open(image_path).convert("RGB")
        img.thumbnail((tile_w - 20, tile_h - 42))
        x = (i % cols) * tile_w + 10
        y = (i // cols) * tile_h + 28
        canvas.paste(img, (x, y))
        draw.text((x, 8 + (i // cols) * tile_h), sheet, fill="black", font=font)
    canvas.save(root / f"CONTACT__{book}.jpg", quality=88)

