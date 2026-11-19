---
name: c69-wallpaper
description: Convert an image or a folder of images into Onyx BOOX C69 sleep-screen wallpapers (758x1024, 16-level grayscale), removing app badges, watermarks and other content unrelated to the picture, with an optional title/credit caption. Use when someone wants pictures prepared for a BOOX C69 / e-ink sleep screen or for Reverie.
---

# C69 wallpaper conversion

These instructions are self-contained and work with any AI assistant or agent that can run a shell command (Claude, ChatGPT, Gemini, local models). A person can also follow them by hand.

## Inputs

Ask for these two if they weren't given:

| Input | Meaning |
|---|---|
| **path** | One image file, or a folder of images. Folders are not searched recursively. Formats: PNG, JPG, WEBP, BMP, GIF (first frame), TIFF. |
| **label** | **yes**: add a two-line caption under the picture (title, then a smaller grey credit). **no**: the picture fills the whole screen with no caption. |

Optional: an output folder. The default is `<input folder>/c69/`. Keep your own temporary files (such as `labels.json`) outside both the image folder and the output folder.

## What the result must be

- 758 × 1024 pixels, portrait, PNG, 16 grey levels with dithering. The script does this.
- Only the picture itself. Remove everything that was added by the app, site or device it came from (see the table below).
- With **label = yes**: a short, accurate caption.

## Tool

`c69_wallpaper.py` sits next to this file. If it's missing, download it from
`https://raw.githubusercontent.com/vijaysingh-dev/reverie-c69/main/skills/c69-wallpaper/c69_wallpaper.py`.

It needs Python 3.8+ and Pillow. Check with `python3 -c "import PIL"`; if that fails, install with `pip install pillow`. On Windows, use `py` instead of `python3`.

Always run the script by its full path (for example `python3 /path/to/c69-wallpaper/c69_wallpaper.py ...`), so the commands work from any folder.

The script automatically removes the Reddit app's bottom badge bar (including a "Posted in r/..." line) and the flat light-grey padding the Reddit app adds above and below some images. Everything else is removed with per-image **crop** and **erase** instructions that you decide on in step 2.

## Steps

### 1. Make grid previews

```
python3 c69_wallpaper.py "<path>" --grid
```

This writes a preview of every source image to `<output folder>/_grid/<file name>_<extension>.jpg`, overlaid with a labelled pixel grid (red numbers are x, blue numbers are y, a line every 100 px, in the original image's pixels). It also prints each image's size and whether a Reddit badge was found. A found badge is removed automatically, but still check the result in step 6.

### 2. Inspect every image and decide what to remove

Look at each grid preview. If you cannot view images, skip to the [no-vision fallback](#if-you-cannot-view-images).

| Remove (unrelated to the picture) | Keep (part of the work) |
|---|---|
| App and share badges or bars: Reddit, Pinterest, Instagram, TikTok, X/Twitter, Tumblr, WhatsApp | The artwork, photo or design itself |
| Usernames and handles (`u/name`, `@name`), "Posted in r/…", "Saved from …" | Text that is part of the design: a quote poster's words, a book title on a cover, a title drawn in the art |
| Site URLs and app logos stamped on the image | The artist's own signature or monogram inside the artwork |
| Phone status bars, navigation bars, buttons and other screenshot UI | Frames and borders drawn as part of the art |
| Like, share and comment counters, captions added by the sharing app | |
| Letterbox padding added by an app, and plain empty margins (optional, see `--trim` below) | |

**Do not remove copyright or stock-photo watermarks** (for example Shutterstock, Getty, Adobe Stock, Alamy, or "© name, all rights reserved"). Removing those is not allowed. Skip such images and tell the user why.

For each image that needs cleaning, choose:

- **crop** `[top, right, bottom, left]`: pixels to cut from each edge. Use this when the unrelated content is at or near an edge. Cutting a little extra is fine.
- **erase** `[[x0, y0, x1, y1], …]`: rectangles painted over with the surrounding colour. (x0, y0) is the top-left corner; x1 and y1 are exclusive, so the box covers x0 to x1-1 and y0 to y1-1. Use this only for overlays sitting on a plain or nearly plain background. Add about 6 px of padding around the content.
- If an overlay sits on detailed artwork and can't be cropped away, don't erase it (it would leave a visible patch). Tell the user and let them decide whether to keep or skip that image.

All coordinates are in the **original** image's pixels, exactly as the grid shows them. Erasing happens before cropping, and the automatic Reddit removal happens after both.

### 3. Write the captions (label = yes only)

For each image:

- **title**: what the picture is, in at most about 40 characters. Examples: "Barn Owl", "Just one more chapter", "The Little Prince". Use the artwork's real title if it's printed on it or you're sure of it. For a quote poster, use the quote or its first words. Don't invent facts.
- **credit**: the artist if a signature or the source says so; otherwise where it came from (for example `r/OnyxBooxC69`); otherwise leave it empty. Don't guess artist names. An empty or missing per-image credit falls back to `--credit` if you pass one.

If the user gave captions, use theirs.

### 4. Write `labels.json`

Put one entry per image that needs a caption, a crop, an erase or a mode. Use the full file name as the key (a name without the extension also works when it's unique). Images without an entry get no crop or erase, and, with label = yes, a title made from the file name (none if the file name is only a code or numbers).

`"mode"` is optional: `"fit"` shows the whole picture (adding margins), `"fill"` fills the area (cutting the edges). By default the script fills only when the picture's shape is within 5% of the screen's, and fits otherwise.

```json
{
  "owl.jpg":    {"title": "Barn Owl", "credit": "Thomas Bewick"},
  "poster.png": {"title": "Just one more chapter", "credit": "r/OnyxBooxC69",
                 "crop": [0, 0, 60, 0]},
  "photo.jpg":  {"title": "Snowy dunes", "credit": "",
                 "erase": [[20, 1840, 400, 1900]]},
  "card.png":   {"title": "The Book Lover", "mode": "fit"}
}
```

Save it somewhere outside the image folder, or delete it afterwards.

### 5. Convert

```
python3 c69_wallpaper.py "<path>" --label --labels labels.json --sheet
python3 c69_wallpaper.py "<path>" --no-label --labels labels.json --sheet
```

Use the first for label = yes and the second for label = no. Every converted image goes to the output folder as `<name>.png` (`<name>_<extension>.png` if two inputs share a name, such as `a.jpg` and `a.png`), and the script prints one line per image saying what it did. `--sheet` also writes `_contact_sheet.png`, a single overview of all results. To redo some images, run the same command again; it overwrites earlier results.

Useful extra options:

| Option | Use |
|---|---|
| `--out <folder>` | Choose the output folder. |
| `--credit "<text>"` | One credit line for every image that has no credit of its own. |
| `--trim` | Also cut plain single-colour margins (letterboxing) before fitting. |
| `--keep-reddit` | Don't remove the Reddit badge. |
| `--font <file.ttf>` | Caption font. The default is DejaVu Serif or Georgia if found. |

For a single file you can skip `labels.json` and pass `--title`, `--credit`, `--crop t,r,b,l` and `--erase x0,y0,x1,y1` (repeatable) directly.

### 6. Check the results

Open `_contact_sheet.png`, or the individual outputs. Check that:

- No badge, handle, URL or UI is left.
- Nothing important was cut off at the edges: a border, a corner ornament, part of the text. If it was, set `"mode": "fit"` for that image.
- No erased patch is noticeable.
- Captions are spelled correctly and not truncated (a truncated caption ends with "…").

Fix `labels.json` and run step 5 again until everything is clean.

### 7. Finish

- Delete `<output folder>/_grid/`, `_contact_sheet.png` and `labels.json` unless the user wants them.
- Report: the output folder, how many images were converted, what was removed from which image, any image you skipped and why, and the captions you used, so the user can correct them.
- To use the results on a C69 with [Reverie](https://github.com/vijaysingh-dev/reverie-c69), copy the PNGs directly into the device's `Pictures` folder.

## If you cannot view images

Run step 5 without `labels.json`. The Reddit badge and padding are still removed automatically. With label = yes, titles come from the file names, and files named only with codes or numbers (such as `RDT_20261119_...` or `IMG_1234`) get no title; pass `--credit` to at least add a source line. Then tell the user that other overlays (watermarks, handles, UI) were not checked, and ask them to look at `_contact_sheet.png` and tell you which images need a crop. They can read the pixel values off the `--grid` previews. Then rerun with their values.

## Output format details

- **Label = yes:** the picture is fitted into a 718 × 936 area, 18 px from the top, on white, with the caption centred below it (title 21 px black, credit 14 px grey).
- **Label = no:** the picture fills 758 × 1024.
- Pictures whose shape is within 5% of the target are scaled to fill it, cropping slightly at the edges. Others are scaled to fit whole, with the rest filled with white (label = yes) or the picture's own edge colour (label = no). `--fit-tolerance` changes the 5%; `"mode"` in `labels.json` overrides it per image.
- Each image gets a slight contrast stretch and sharpening, then is reduced to the 16 grey levels the e-ink panel can show, with Floyd–Steinberg dithering.
