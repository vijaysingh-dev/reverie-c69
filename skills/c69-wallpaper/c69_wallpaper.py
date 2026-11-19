#!/usr/bin/env python3
"""
c69_wallpaper.py - convert images into Onyx BOOX C69 sleep-screen wallpapers.

Output: 758x1024 portrait PNG, 16-level grayscale (the e-ink panel's depth), dithered.
Optionally adds a two-line caption (title + credit) under the picture.
Removes unrelated content: the Reddit app's badge bar automatically; anything else (watermarks,
usernames, site URLs, "Posted in ..." lines, app/UI bars) through per-image "crop" and "erase" boxes,
usually chosen by looking at --grid previews.

Requires Python 3.8+ and Pillow (pip install pillow). No other dependencies.

Examples
  python3 c69_wallpaper.py photo.jpg --label --title "Barn Owl" --credit "Thomas Bewick"
  python3 c69_wallpaper.py ./my-images --no-label
  python3 c69_wallpaper.py ./my-images --grid          # previews with pixel grid, no conversion
  python3 c69_wallpaper.py ./my-images --label --labels labels.json --sheet
Run with -h for all options.
"""
import argparse
import json
import os
import re
import sys

try:
    from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageOps, ImageSequence
except ImportError:
    sys.exit("Pillow is required: pip install pillow")

W, H = 758, 1024            # C69 screen, portrait
CAP_H = 60                  # caption band height (label mode)
MARGIN_X, MARGIN_TOP = 20, 18
EXTS = {".png", ".jpg", ".jpeg", ".webp", ".bmp", ".gif", ".tif", ".tiff"}
FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
    "/usr/share/fonts/dejavu/DejaVuSerif.ttf",
    "/usr/share/fonts/TTF/DejaVuSerif.ttf",
    "/System/Library/Fonts/Supplemental/Georgia.ttf",
    "/Library/Fonts/Georgia.ttf",
    "C:/Windows/Fonts/georgia.ttf",
    "C:/Windows/Fonts/times.ttf",
]


# ---------- loading ----------

def load_rgb(path):
    im = Image.open(path)
    if getattr(im, "is_animated", False):
        im = next(ImageSequence.Iterator(im)).copy()
    im = ImageOps.exif_transpose(im)
    if im.mode in ("RGBA", "LA", "P", "PA"):
        im = im.convert("RGBA")
        bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
        im = Image.alpha_composite(bg, im)
    return im.convert("RGB")


# ---------- badge / border removal ----------

def _close(a, b, tol):
    return abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2]) <= tol


def reddit_badge_height(im):
    """Height in px of the Reddit app's bottom badge bar, or 0 if there is none.

    The bar is a solid strip along the bottom (dark or grey) holding the orange-red
    reddit logo, sometimes with 'Posted in r/...' text. Detected as a bottom band of
    one colour that contains the logo's orange pixels.
    """
    w, h = im.size
    px = im.load()
    step = max(1, w // 300)
    xs = range(0, w, step)
    bar = px[w // 2, h - 2]
    if not all(_close(px[x, h - 2], bar, 30) for x in (2, w // 4, w - 3)):
        return 0
    # Walk up through rows that are mostly (>= 60%) bar colour. Rows crossing the logo or a
    # "Posted in r/..." line are less, so a gap of up to 40 such rows is bridged - but only when
    # bar-coloured rows continue above it; otherwise the band ends below the gap.
    top, misses = h - 1, 0
    for y in range(h - 1, int(h * 0.75), -1):
        same = sum(1 for x in xs if _close(px[x, y], bar, 30)) / len(xs)
        if same >= 0.6:
            top, misses = y, 0
        else:
            misses += 1
            if misses > 40:
                break
    band = h - top
    if band < max(12, h * 0.015) or band > h * 0.2:
        return 0
    orange = 0
    for y in range(top, h, max(1, band // 40)):
        for x in range(w // 2, w, step):
            r, g, b = px[x, y]
            if r > 190 and 30 < g < 130 and b < 90 and r - g > 100:
                orange += 1
    return band if orange >= 3 else 0


REDDIT_PAD = (228, 228, 228)


def strip_flat_bands(im, tol=6):
    """Remove the flat light-grey (228) letterbox rows the Reddit app adds above and below some
    images. Only used after a Reddit badge was found; other flat colours belong to the artwork."""
    px = im.load()
    w, h = im.size
    xs = range(0, w, max(1, w // 200))

    def flat(y):
        return all(_close(px[x, y], REDDIT_PAD, tol) for x in xs)

    top = 0
    while top < h * 0.25 and flat(top):
        top += 1
    bottom = h
    while bottom > h * 0.75 and flat(bottom - 1):
        bottom -= 1
    # a found band gets 2 extra rows: JPEG blurs the edge between padding and picture
    top = 0 if top < 3 else top + 2
    bottom = h if h - bottom < 3 else bottom - 2
    return im.crop((0, top, w, bottom)) if (top or bottom != h) else im, top, h - bottom


def erase_boxes(im, boxes):
    """Paint each [x0, y0, x1, y1] box with the median colour of a thin ring around it."""
    px = im.load()
    w, h = im.size
    for x0, y0, x1, y1 in boxes:
        x0, y0, x1, y1 = max(0, x0), max(0, y0), min(w, x1), min(h, y1)
        ring = []
        for x in range(max(0, x0 - 6), min(w, x1 + 6), 2):
            for y in (max(0, y0 - 4), min(h - 1, y1 + 3)):
                ring.append(px[x, y])
        for y in range(max(0, y0 - 6), min(h, y1 + 6), 2):
            for x in (max(0, x0 - 4), min(w - 1, x1 + 3)):
                ring.append(px[x, y])
        if not ring:
            continue
        fill = tuple(sorted(c[i] for c in ring)[len(ring) // 2] for i in range(3))
        ImageDraw.Draw(im).rectangle([x0, y0, x1 - 1, y1 - 1], fill=fill)
    return im


def grid_preview(im, dest, step=100):
    """Source image with a labelled pixel grid, so crop/erase boxes can be read off it."""
    g = im.convert("RGB")
    scale = min(1.0, 1400 / max(g.size))
    if scale < 1:
        g = g.resize((round(g.width * scale), round(g.height * scale)))
    d = ImageDraw.Draw(g)
    w, h = g.size
    for v in range(0, max(im.size), step):
        p = round(v * scale)
        if v < im.width:
            d.line([(p, 0), (p, h)], fill=(255, 0, 0), width=1)
            d.text((p + 2, 2), str(v), fill=(255, 0, 0))
        if v < im.height:
            d.line([(0, p), (w, p)], fill=(0, 120, 255), width=1)
            d.text((2, p + 2), str(v), fill=(0, 120, 255))
    d.text((w - 140, h - 14), f"{im.width}x{im.height}", fill=(255, 0, 0))
    g.save(dest, quality=85)


def trim_uniform_border(im, tol=18):
    """Trim solid-colour margins (letterbox bars) on all four sides."""
    g = im.convert("L")
    w, h = g.size
    corner = g.getpixel((0, 0))
    mask = g.point(lambda v: 255 if abs(v - corner) > tol else 0)
    box = mask.getbbox()
    if not box or (box[2] - box[0]) < w * 0.3 or (box[3] - box[1]) < h * 0.3:
        return im
    return im.crop(box)


# ---------- rendering ----------

def find_font(path=None):
    for p in ([path] if path else []) + FONT_CANDIDATES:
        if p and os.path.exists(p):
            return p
    return None


def load_font(path, size):
    if path:
        return ImageFont.truetype(path, size)
    try:
        return ImageFont.load_default(size)        # Pillow >= 10.1
    except TypeError:
        return ImageFont.load_default()


def fit_text(draw, text, font, max_w):
    if draw.textlength(text, font=font) <= max_w:
        return text
    while text and draw.textlength(text + "\u2026", font=font) > max_w:
        text = text[:-1]
    return text.rstrip() + "\u2026"


def border_median(g):
    w, h = g.size
    vals = []
    for x in range(0, w, max(1, w // 100)):
        vals += [g.getpixel((x, 0)), g.getpixel((x, h - 1))]
    for y in range(0, h, max(1, h // 100)):
        vals += [g.getpixel((0, y)), g.getpixel((w - 1, y))]
    vals.sort()
    return vals[len(vals) // 2]


def gray16_palette():
    pal = Image.new("P", (1, 1))
    levels = [round(i * 255 / 15) for i in range(16)]
    pal.putpalette(sum([[v, v, v] for v in levels], []) + [0] * (768 - 48))
    return pal


def render(im, label, title, credit, font_path, fit_tol, force=None):
    g = im.convert("L")
    if label:
        aw, ah, top = W - 2 * MARGIN_X, H - CAP_H - 28, MARGIN_TOP
    else:
        aw, ah, top = W, H, 0
    ratio, target = g.height / g.width, ah / aw
    if force == "fill" or (force != "fit" and abs(ratio - target) / target <= fit_tol):
        art, mode = ImageOps.fit(g, (aw, ah), Image.LANCZOS), "fill"
    else:
        art, mode = ImageOps.contain(g, (aw, ah), Image.LANCZOS), "fit"
    art = ImageOps.autocontrast(art, cutoff=0.3)
    art = art.filter(ImageFilter.UnsharpMask(radius=1, percent=40, threshold=2))
    bg = 255 if label else border_median(g)
    canvas = Image.new("L", (W, H), bg)
    canvas.paste(art, ((W - art.width) // 2, top + (ah - art.height) // 2))
    if label and (title or credit):
        d = ImageDraw.Draw(canvas)
        if title:
            f = load_font(font_path, 21)
            d.text((W // 2, H - CAP_H + 10), fit_text(d, title, f, W - 60), font=f, fill=0, anchor="mt")
        if credit:
            f = load_font(font_path, 14)
            d.text((W // 2, H - CAP_H + 38), fit_text(d, credit, f, W - 60), font=f, fill=90, anchor="mt")
    out = canvas.convert("RGB").quantize(palette=gray16_palette(), dither=Image.Dither.FLOYDSTEINBERG)
    return out.convert("L"), mode


# ---------- helpers ----------

def title_from_filename(name):
    stem = os.path.splitext(os.path.basename(name))[0]
    stem = re.sub(r"^(RDT|IMG|PXL|Screenshot|DSC|PHOTO|image)[_-]?", "", stem, flags=re.I)
    stem = re.sub(r"\d{5,}", " ", stem)                  # timestamps / ids
    stem = re.sub(r"[_\-.]+", " ", stem)
    stem = re.sub(r"\s+", " ", stem).strip()
    return stem[:1].upper() + stem[1:] if stem else ""


def parse_crop(s):
    if s is None:
        return None
    if isinstance(s, (list, tuple)):
        vals = [int(v) for v in s]
    else:
        vals = [int(v) for v in str(s).split(",")]
    if len(vals) != 4:
        raise ValueError("crop must be 4 numbers: top,right,bottom,left")
    return vals


def collect(path):
    if os.path.isfile(path):
        return [path]
    if os.path.isdir(path):
        return sorted(os.path.join(path, f) for f in os.listdir(path)
                      if os.path.splitext(f)[1].lower() in EXTS and os.path.isfile(os.path.join(path, f)))
    sys.exit(f"Not found: {path}")


def contact_sheet(files, dest, cols=6, tw=190):
    th = round(tw * H / W)
    rows = (len(files) + cols - 1) // cols
    sheet = Image.new("L", (cols * tw, rows * (th + 18)), 160)
    d = ImageDraw.Draw(sheet)
    for i, f in enumerate(files):
        x, y = (i % cols) * tw, (i // cols) * (th + 18)
        sheet.paste(Image.open(f).convert("L").resize((tw, th)), (x, y))
        n = os.path.basename(f)
        d.text((x + 3, y + th + 2), n if len(n) <= 30 else "\u2026" + n[-29:], fill=0)
    sheet.save(dest)


# ---------- main ----------

def main():
    ap = argparse.ArgumentParser(description="Convert images to Onyx BOOX C69 sleep-screen wallpapers (758x1024, 16-gray).")
    ap.add_argument("path", help="an image file or a folder of images (not recursive)")
    lab = ap.add_mutually_exclusive_group()
    lab.add_argument("--label", dest="label", action="store_true", help="add a caption (title + credit) under the picture")
    lab.add_argument("--no-label", dest="label", action="store_false", help="full-screen picture, no caption")
    ap.add_argument("--out", help="output folder (default: <input folder>/c69)")
    ap.add_argument("--title", help="caption title (single-file input)")
    ap.add_argument("--credit", help="caption credit line, e.g. artist or source (applies to all files without their own)")
    ap.add_argument("--labels", help='JSON file: {"<file name or stem>": {"title": "...", "credit": "...", '
                                     '"crop": [t,r,b,l], "erase": [[x0,y0,x1,y1], ...], "mode": "fit"|"fill"}}')
    ap.add_argument("--crop", help="pixels to cut from the source: top,right,bottom,left (single-file input)")
    ap.add_argument("--erase", action="append", default=[],
                    help="box x0,y0,x1,y1 to paint over with the surrounding colour (single-file input; repeatable)")
    ap.add_argument("--grid", action="store_true",
                    help="only write source previews with a pixel grid to <out>/_grid/ (no conversion)")
    ap.add_argument("--trim", action="store_true", help="also trim solid-colour margins around the picture")
    ap.add_argument("--keep-reddit", action="store_true", help="do not remove the Reddit badge bar")
    ap.add_argument("--fit-tolerance", type=float, default=0.05,
                    help="fill (crop slightly) instead of fit when aspect ratios differ by at most this fraction (default 0.05)")
    ap.add_argument("--font", help="TTF/OTF font for captions (default: DejaVu Serif / Georgia if found)")
    ap.add_argument("--sheet", action="store_true", help="also write _contact_sheet.png in the output folder for review")
    ap.set_defaults(label=None)
    a = ap.parse_args()
    if a.label is None and not a.grid:
        ap.error("choose --label or --no-label")

    files = collect(a.path)
    if not files:
        sys.exit("No images found.")
    in_dir = a.path if os.path.isdir(a.path) else (os.path.dirname(a.path) or ".")
    out_dir = a.out or os.path.join(in_dir, "c69")
    os.makedirs(out_dir, exist_ok=True)
    labels = {}
    if a.labels:
        with open(a.labels, encoding="utf-8") as fh:
            labels = json.load(fh)
    font_path = find_font(a.font)
    if a.label and not font_path:
        print("note: no serif font found, using Pillow's built-in font (pass --font for a nicer caption)")

    if a.grid:
        gdir = os.path.join(out_dir, "_grid")
        os.makedirs(gdir, exist_ok=True)
        for f in files:
            stem = os.path.splitext(os.path.basename(f))[0]
            im = load_rgb(f)
            grid_preview(im, os.path.join(gdir, os.path.basename(f).replace(".", "_") + ".jpg"))
            bh = reddit_badge_height(im)
            print(f"GRID  {os.path.basename(f)} {im.width}x{im.height}" + (f" (reddit badge: bottom {bh}px, auto-removed)" if bh else ""))
        print(f"grid previews -> {gdir}")
        return

    stems = [os.path.splitext(os.path.basename(f))[0] for f in files]
    written = []
    for f in files:
        base = os.path.basename(f)
        stem = os.path.splitext(base)[0]
        out_stem = stem if stems.count(stem) == 1 else base.replace(".", "_")
        meta = labels.get(base) or labels.get(stem) or {}
        try:
            im = load_rgb(f)
        except Exception as e:
            print(f"SKIP  {base}: {e}")
            continue
        notes = []
        # All manual coordinates refer to the ORIGINAL image (as in the --grid preview): erase, then crop.
        boxes = meta.get("erase") or ([[int(v) for v in e.split(",")] for e in a.erase] if len(files) == 1 else [])
        if boxes:
            im = erase_boxes(im, boxes)
            notes.append(f"erased {len(boxes)} box(es)")
        crop = parse_crop(meta.get("crop")) or (parse_crop(a.crop) if len(files) == 1 else None)
        if crop:
            t, r, b, l = crop
            im = im.crop((l, t, im.width - r, im.height - b))
            notes.append(f"crop {t},{r},{b},{l}")
        if not a.keep_reddit:
            bh = reddit_badge_height(im)
            if bh:
                im = im.crop((0, 0, im.width, im.height - bh))
                notes.append(f"reddit badge -{bh}px")
                im, t_cut, b_cut = strip_flat_bands(im)
                if t_cut or b_cut:
                    notes.append(f"reddit padding -{t_cut}px top/-{b_cut}px bottom")
        if a.trim:
            before = im.size
            im = trim_uniform_border(im)
            if im.size != before:
                notes.append("trimmed margins")
        title = meta.get("title")
        if title is None:
            title = a.title if (a.title and len(files) == 1) else title_from_filename(base)
        credit = meta.get("credit") or a.credit or ""
        out, mode = render(im, a.label, title, credit, font_path, a.fit_tolerance, meta.get("mode"))
        dest = os.path.join(out_dir, out_stem + ".png")
        if os.path.abspath(dest) == os.path.abspath(f):
            dest = os.path.join(out_dir, stem + "_c69.png")
        out.save(dest, optimize=True)
        written.append(dest)
        cap = f' | caption: "{title}"' + (f' / "{credit}"' if credit else "") if a.label else ""
        print(f"OK    {base} -> {os.path.basename(dest)} ({mode}{', ' + ', '.join(notes) if notes else ''}){cap}")

    if a.sheet and written:
        contact_sheet(written, os.path.join(out_dir, "_contact_sheet.png"))
        print(f"sheet {os.path.join(out_dir, '_contact_sheet.png')}")
    print(f"done: {len(written)} of {len(files)} image(s) -> {out_dir}")


if __name__ == "__main__":
    main()
