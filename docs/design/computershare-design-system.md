# Computershare (Public Site) — Design System

*Extracted from a saved snapshot of `computershare.com/uk` (homepage). Source: `Computershare UK.html` + `computershare-066e3c47e04121232a8d3c4573ccbac0.css` (421KB, minified). This is a reverse-engineered extraction, not an authored spec — see the caveat in §11.*

---

## 1. Brand Identity

Computershare's public marketing site is a **light, editorial-corporate** design: white/near-white surfaces, a magenta-to-purple brand gradient as the single strong accent, generous card radii, and a Swiper-driven carousel layout for homepage storytelling (hero → value-prop cards → news → CTA). It reads as a financial-services marketing site, not a dense enterprise application — the opposite end of the spectrum from a data-grid product UI.

**Design principles (as observed, not officially documented):**
- White/light-neutral surfaces as the default — no dark mode
- A two-color brand gradient (magenta → deep purple) is the signature visual device, used on hero overlays, CTA modules, and the login/language buttons
- Large, soft-rounded cards (20–32px radius) over sharp edges
- One typeface (Manrope) for everything — no serif, no monospace, no secondary display face
- Carousel/expanding-card patterns (Swiper.js) drive the homepage narrative rather than a static grid
- Logo swaps light/dark depending on scroll and menu state, not just breakpoint

---

## 2. Colour Palette

**Note: no formal token system exists.** The CSS defines almost no true `--custom-property` design tokens — colors are hardcoded per-selector throughout the file. The palette below is reconstructed by frequency analysis of actual hex usage, not read off a declared token list. Treat the "role" column as inferred, not authoritative.

### Brand
| Colour | Hex | Frequency | Usage |
|---|---|---|---|
| **Primary — Magenta** | `#93186c` | 158× | Text links, active nav, headings, brand backgrounds, gradient stop, `.btn-arrow`, accordion active state |
| **Secondary — Deep Purple** | `#432063` | 121× | Paired with primary in the signature gradient; toggle/button text color |
| Darker magenta shade | `#781559` | 8× | Hover/pressed variant of primary |
| Accent violet | `#b922f3` | 37× | Decorative gradients, external-link icon accents |

**Signature brand gradient:**
```css
background: linear-gradient(90deg, #432063 0%, #93186c 100%);
```
Used on `.cta-module__inner`; a steeper-angle variant `linear-gradient(257deg, rgba(237,240,244,0) -71.44%, #93186c 39.36%)` is used for featured-article image overlays.

### Neutral / Surface
| Colour | Hex | Frequency | Usage |
|---|---|---|---|
| Light neutral | `#f0efef` | 68× | Mega-menu panel, search overlay background |
| Light neutral (alt) | `#edf0f4` | 9× | Secondary light background |
| Light grey | `#f5f5f5` | 5× | Misc light background |
| Neutral grey (text) | `#8e9298` | 6× | Secondary/muted text |
| White | `#fff` | 174× | Primary surface, nav-scrolled/logo-on-dark text |
| Black | `#000` | 24× | Rare — mostly in shadow rgba, not solid fills |

### Accent (secondary, lower frequency)
| Colour | Hex | Frequency | Usage |
|---|---|---|---|
| Orange | `#ff9900` | 17× | Gradient stop / warning-adjacent accent |
| Coral | `#e8724e` | 4× | Occasional accent |
| Blue | `#003cac` | 4× | Occasional accent, not a systematic status color |

### Overlay / Tint (rgba)
| Pattern | Usage |
|---|---|
| `rgba(67,32,99,.1 / .3 / .5 / .8)` | Purple tint overlays, hover/scrim states |
| `rgba(147,24,108,.05)` | Magenta tint, subtle hover background |
| `rgba(16,24,40,.03 / .08)` | Shadow-family greys (card elevation) |
| `rgba(12,12,13,.2)` | Inset image-overlay scrim |

**No semantic/status colors** (success/warning/error/info) were found in the extracted CSS — this homepage snapshot has no forms, alerts, or data tables that would need them. Do not assume they don't exist elsewhere on the site; they simply aren't present in this page.

---

## 3. Typography

**Font:** Single family site-wide, loaded (not system-native):
```css
font-family: "Manrope", sans-serif;
```
One isolated fallback case of `"Arial", sans-serif` was found — likely a CMS rich-text default, not intentional design.

No secondary/display typeface, no monospace face.

### Heading scale (responsive — mobile value → desktop value at the 1200px breakpoint)
| Class | Mobile | Desktop | Line-height | Notes |
|---|---|---|---|---|
| `.h1` | 2.75rem (44px) | 3.75rem (60px) | 1.1 | |
| `.h2` | 2.5rem (40px) | 3.5rem (56px) | 1.3 | `letter-spacing: -0.56px` → `-0.784px`; brand-colored (`#93186c`) on some variants |
| `.h3` | 2.25rem (36px) | 3rem (48px) | 1.1 | |

### Weights in use
700 (most common — headings, buttons), 400 (body), 600, 500, and one isolated 200 (light) instance.

### UI text examples (real, extracted)
| Element | Size | Weight | Line-height | Notes |
|---|---|---|---|---|
| `.login-button` | 14px (.875rem) | 700 | 1.4286 | |
| `.footer__nav-link` | — | 400 | 1.5 | color `#fff` |
| `.footer__legal-link` | 12px (.75rem) | 400 | 1.5 | |
| `.btn-arrow` | 16px (1rem) | 700 | 1.25 | |
| `.button` (generic CMS CTA) | 16px | bold | — | `text-transform: uppercase` |
| `.tag-eyebrow` | 14px | — | 1.4 | uppercase, `letter-spacing: .28px` |

---

## 4. Spacing

**Note: no `--spacing-*` tokens declared.** Values cluster tightly around an **8px base unit**, confirmed by frequency analysis of `gap`/`padding`/`margin`:

```
4px · 8px · 12px · 16px · 24px · 32px · 40px · 48px · 56px · 64px · 80px · 96px · 120px
```
(10px, 20px, 44px also appear but off-scale — likely one-off CMS values, not part of the systematic rhythm.)

Most common single values: `gap: 24px` (91×), `gap: 16px` (50×), `gap: 32px` (44×), `gap: 8px` (36×).

**Responsive section padding** follows a distinct three-step pattern at the site's breakpoints:
```css
.promo-list { padding: 44px 0; }
@media (min-width: 768px)  { .promo-list { padding: 56px 0; } }
@media (min-width: 1200px) { .promo-list { padding: 80px 0; } }
```

---

## 5. Components

*Only components actually present in this homepage snapshot are documented below — nothing invented. Forms, data tables, alerts/toasts, pagination, and breadcrumbs were not found on this page and are not assumed to be absent site-wide.*

### 5.1 Top Navigation
```css
.navigation {
  position: absolute;
  z-index: 8888;
  transition: background-color .4s ease-in-out;
}
/* height swaps via a --height-navigation custom property: 72px mobile → 88px at 1200px */
```
Logo swaps light/dark by nav state (`.navigation--dark-theme`, `.navigation-opening`, `:hover`, `.navigation--pinned`) via `.navigation__logo--dark` / `.navigation__logo--light` `display:none`/`block` toggling — a light-on-hero → dark-on-scrolled pattern, not a responsive breakpoint swap.

### 5.2 Personal / Business Toggle
Pill-shaped segmented control.
```css
.personal-business-toggle__button {
  border-radius: 100px;
  padding: 12px 24px;
  width: 50%;
  color: #432063;
  transition: all .2s ease;
}
```

### 5.3 Login Button + Mega-Dropdown
```css
.login-button {
  font-weight: 700;
  color: #93186c;
  border-radius: 100px;
  padding: 12px 20px;
}
.login-dropdown-list { padding: 24px; }
```

### 5.4 Mega-Menu
```css
.mega-menu {
  background: #f0efef;
  border-radius: 20px;
}
.mega-menu__container { max-width: 1420px; }
```

### 5.5 Search Overlay
```css
.search-box {
  background: #f0efef;
  border-radius: 20px;
  padding: 40px;
  /* animated via opacity/transform, transition: .4s ease */
}
```

### 5.6 Hero / Personal Banner
`.personal-banner`, `__hero`, `__hero-title`, `__cards`, `__card`, `__card--dark` — homepage hero with a background image and a card row beneath it.

### 5.7 Value-Prop Cards (expanding carousel)
```css
.value-prop-card {
  border-radius: 24px;
  /* brand-color backgrounds: #93186c / #432063 */
}
/* modifiers: --active, --fade */
```

### 5.8 Tabs / Accordion
`.tab-content__tabs`, `.common-tab`, `.scroll-tabs` for tabbed content; `.accordion-blocks-faq__question` (color `#93186c`), `__header`, `__icon` for the FAQ accordion.

### 5.9 CTA Module
```css
.cta-module__inner {
  background: linear-gradient(90deg, #432063 0%, #93186c 100%);
  border-radius: 24px; /* up to 32px on larger variants */
  padding: 32px;
  /* responsive up to padding: 80px 64px */
}
```

### 5.10 Card
```css
.card {
  background: #fff;
  border-radius: 24px;
  padding: 24px; /* up to 48px */
  margin: 32px auto; /* up to 60px */
}
```

### 5.11 Footer
```css
.footer__main { display: flex; gap: 154px; } /* desktop */
.footer__nav-link { color: #fff; font-weight: 400; }
.footer__legal-links { gap: 16px; }
.footer__legal-link { font-size: .75rem; }
```
Also: `__social-link`, `__copyright`, `__brand-column`.

### 5.12 Language Selector
```css
.language-selector__button {
  background-color: #93186c;
  color: #fff;
  padding: 12px;
}
```

### 5.13 Cookie / Consent Banner (Evidon, third-party)
```css
.evidon-banner-message {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
}
```
Note: this is a third-party vendor component (Evidon/Sourcepoint CMP), not part of Computershare's own design system — styled to match, but not authored in-house.

### 5.14 Generic CMS Button
```css
.button {
  font-size: 16px;
  padding: 13px 26px;
  border-width: 2px;
  border-radius: 7px; /* off-scale — not 8/16/24 like the rest of the system */
  text-transform: uppercase;
  font-weight: bold;
}
```
Variants suggest a rich-text-editor button picker: `.cpu-rteElement-buttongradient`, `-buttonwhite`, `-buttonwhiteoutline`. This is visually and structurally distinct from `.login-button` and the toggle's pill-button style — likely a legacy/CMS-authored button, not the primary design-system button.

### 5.15 Arrow Link
```css
.btn-arrow {
  color: #93186c;
  font-weight: 700;
  display: inline-flex;
  gap: 8px;
}
```

### 5.16 Tag / Eyebrow Label
```css
.tag-eyebrow {
  text-transform: uppercase;
  padding: 6px 12px;
  letter-spacing: .28px;
}
```

### 5.17 Carousel (Swiper.js, vendor)
`.swiper`, `.swiper-slide`, `.swiper-pagination`, `.swiper-button-prev/next` — third-party carousel library powering the value-prop and news sections. `--swiper-theme-color: #007aff` is the vendor default and is unused; brand colors override it inline per-instance.

---

## 6. Iconography

- Icons are inline SVG, `fill="currentColor"` — inherit surrounding text color rather than being hardcoded
- External-link icons use a two-stop orange→violet gradient (`#ff9900` → `#b922f3`) via an SVG `<linearGradient>`, distinct from the flat-color pattern used elsewhere
- Chevron/caret icons (e.g. `chevron-down.svg`) are simple monochrome outline paths

---

## 7. Elevation

No `--shadow-*` tokens exist; several recurring shadow patterns act as de facto elevation levels:

```css
/* Standard card elevation — 4 occurrences */
box-shadow: 0 12px 16px -4px rgba(16,24,40,.08), 0 4px 6px -2px rgba(16,24,40,.03);

/* Inset image/overlay scrim — 3 occurrences */
box-shadow: inset 0 4px 24px 0 rgba(12,12,13,.2), inset 0 0 0 1000px rgba(12,12,13,.2);

/* Misc CTA/button shadow */
box-shadow: 0px 4px 10px 0px rgba(0,0,0,.25);
box-shadow: 0 4px 20px rgba(0,0,0,.1);
```

---

## 8. Motion

No `--transition-*` tokens exist; the de facto standard is:
```css
transition: all .3s ease;          /* 23× — default interactive transition */
transition: all .3s ease-in-out;   /* 14× */
transition: all .2s ease;          /* 10× — faster/lighter interactions (toggle) */
transition: transform .3s ease-in-out; /* 7× — carousel/card movement */
```

---

## 9. Logo Usage

**Assets:** `computershare-logo-dark.svg` and `computershare-logo-white.svg` (both ~10.6KB), present in the page's asset bundle.

**Behavior (not just responsive):** the two logos are swapped based on **navigation state**, not screen size — `.navigation__logo--dark` and `.navigation__logo--light` toggle `display: none`/`block` depending on whether the nav is transparent-over-hero, hovered, scrolled/pinned, or menu-open. This is a light-logo-on-dark-hero → dark-logo-on-scrolled-white pattern.

No minimum-clear-space or minimum-size guidance could be extracted from CSS alone — that would require the brand guidelines document, not this page.

---

## 10. Page Layout

```
┌──────────────────────────────────────────────────────┐  ← Nav (absolute, transparent→white, 72–88px)
│  [Logo]      Personal | Business    Login ▾  [Region] │
├──────────────────────────────────────────────────────┤
│                                                        │
│               Hero (background image)                 │  ← .personal-banner
│               + card row beneath                      │
│                                                        │
├──────────────────────────────────────────────────────┤
│         Value-prop cards (Swiper carousel)             │  ← .value-prop-card, 24px radius
├──────────────────────────────────────────────────────┤
│         Content sections (tabs / accordion / cards)    │
├──────────────────────────────────────────────────────┤
│         CTA module (brand gradient, 24–32px radius)     │
├──────────────────────────────────────────────────────┤
│  Footer — nav columns (gap 154px desktop) + legal row   │
└──────────────────────────────────────────────────────┘
```
Container max-width observed: `1420px` (mega-menu container).

**Breakpoints in use:**
| Width | Occurrences | Role |
|---|---|---|
| 768px | 296 | Tablet |
| 992px | 14 | Minor/rare |
| 1200px | 431 | **Primary desktop breakpoint** — most components switch here |
| 1440px | 11 | Large-desktop / max-width contexts |

---

## 11. Caveats — read before treating this as authoritative

This document was reverse-engineered from a single saved homepage snapshot via automated `grep` extraction against a 421KB minified CSS file, not read from an official brand/design-system source:

- **No true design tokens exist in the source.** Almost no `--custom-property` variables were found; colors, spacing, radii, and shadows are hardcoded per-selector throughout. The "palette," "spacing scale," and "elevation" sections above are *inferred from frequency analysis*, not declared by Computershare's own tooling.
- **Single-page scope.** Only components that appear on the UK homepage are documented — forms, data tables, alerts/toasts, pagination, and breadcrumbs were not present in this snapshot and are neither confirmed nor assumed absent elsewhere on the site.
- **The Evidon cookie banner and Swiper carousel are third-party vendor components**, styled to match the brand but not part of Computershare's own component library.
- If a future task needs this to be authoritative (e.g. building new EquatePlus-aligned UI against it), treat this as a **starting hypothesis to validate against Computershare's actual brand guidelines or a live design-system export**, not a source of truth on its own.
