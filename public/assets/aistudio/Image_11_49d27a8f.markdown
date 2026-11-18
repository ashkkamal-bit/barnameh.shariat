---
name: Nocturne Intelligence
colors:
  surface: '#0c1322'
  surface-dim: '#0c1322'
  surface-bright: '#323949'
  surface-container-lowest: '#070e1d'
  surface-container-low: '#141b2b'
  surface-container: '#191f2f'
  surface-container-high: '#232a3a'
  surface-container-highest: '#2e3545'
  on-surface: '#dce2f7'
  on-surface-variant: '#cbc3d7'
  inverse-surface: '#dce2f7'
  inverse-on-surface: '#293040'
  outline: '#958ea0'
  outline-variant: '#494454'
  surface-tint: '#d0bcff'
  primary: '#d0bcff'
  on-primary: '#3c0091'
  primary-container: '#a078ff'
  on-primary-container: '#340080'
  inverse-primary: '#6d3bd7'
  secondary: '#4cd7f6'
  on-secondary: '#003640'
  secondary-container: '#03b5d3'
  on-secondary-container: '#00424e'
  tertiary: '#ffb95f'
  on-tertiary: '#472a00'
  tertiary-container: '#ca8100'
  on-tertiary-container: '#3e2400'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e9ddff'
  primary-fixed-dim: '#d0bcff'
  on-primary-fixed: '#23005c'
  on-primary-fixed-variant: '#5516be'
  secondary-fixed: '#acedff'
  secondary-fixed-dim: '#4cd7f6'
  on-secondary-fixed: '#001f26'
  on-secondary-fixed-variant: '#004e5c'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#0c1322'
  on-background: '#dce2f7'
  surface-variant: '#2e3545'
typography:
  display-lg:
    fontFamily: Vazirmatn
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 52px
    letterSpacing: '0'
  headline-lg:
    fontFamily: Vazirmatn
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: '0'
  headline-lg-mobile:
    fontFamily: Vazirmatn
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: '0'
  headline-md:
    fontFamily: Vazirmatn
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: '0'
  title-lg:
    fontFamily: Vazirmatn
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: '0'
  title-md:
    fontFamily: Vazirmatn
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: '0'
  body-lg:
    fontFamily: Vazirmatn
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: '0'
  body-md:
    fontFamily: Vazirmatn
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: '0'
  label-lg:
    fontFamily: Vazirmatn
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: '0'
  label-md:
    fontFamily: Vazirmatn
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: '0'
  label-sm:
    fontFamily: Vazirmatn
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: '0'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system establishes a high-performance, RTL-first Android experience tailored for high-agency Persian speakers seeking calm mastery over their daily schedules. It fuses deep nocturnal surfaces with electric, hyper-responsive cognitive cues, evoking clarity, technological mastery, and psychological composure. 

The aesthetic is an evolved synthesis of **Material 3 Surface Architecture** and **Luminous Glassmorphism**. Dark slate planes mitigate eye fatigue, while focused cyan and electric violet micro-glows signal ambient AI active listening, task synthesis, and automated time blocking. Every visual asset respects native right-to-left layout mechanics, honoring Persian calligraphic rhythms and ergonomic single-handed thumb zones.

## Colors

The palette leverages a nocturnal continuum to emphasize luminous visual feedback without excessive brightness:

- **Surface Ground & Foundations:** Canvas ground begins at `#0B0F19` (Void Black), stepping up through tonal elevations: `#111827` (Base Slate), `#1E2235` (Surface Container Low), `#1F2937` (Surface Container), and `#2D3748` (Surface Container High).
- **Primary & AI Resonance:** `#8B5CF6` (Electric Violet) acts as the dominant cognitive accent, paired with `#6366F1` (Indigo Glow) to convey intelligent synthesis and voice state processing.
- **Secondary & Telemetry:** `#06B6D4` (Luminous Cyan) provides high-contrast data readouts, real-time waveform oscillations, audio confidence meters, and time stamps.
- **Status & Priority:**
  - High Priority / Urgent: `#EF4444` (Signal Coral Crimson)
  - Medium Priority / Dynamic Reschedule: `#F59E0B` (Warm Solar Amber)
  - Task Completion / Success: `#10B981` (Soft Emerald Jade)
- **Text & Foreground:** Primary text renders in `#F9FAFB` (95% opacity), secondary contextual metadata in `#9CA3AF` (70% opacity), and subtle system dividers in `#374151` (40% opacity).

## Typography

Typography relies on Vazirmatn, optimized for screen clarity, balance, and native Persian ligature positioning. 

- **Numerical Uniformity:** All calendar dates, Jalali hours, and countdown timers must leverage Persian digits (`۰-۹`) using tabular alignment (`tnum`) to eliminate micro-shifts during dynamic audio playback or real-time voice streaming.
- **RTL Baseline Alignment:** Persian glyphs exhibit a lower descending axis than Latin typefaces. Line-heights are intentionally expanded by 4px-8px across all tiers to prevent clipping on diacritics and ascenders.
- **Hierarchy Mapping:**
  - `display-lg`: Daily greeting and voice transcription feedback heads.
  - `headline-lg-mobile`: Screen-level titles (e.g., برنامه امروز).
  - `title-md`: Task card titles and Jalali week headers.
  - `body-md`: Detailed AI notes, schedule breakdowns, and subtasks.
  - `label-sm`: Micro badge tags, priority indicators, and audio timestamps.

## Layout & Spacing

The spatial architecture strictly conforms to an **8pt fluid vertical grid** paired with safe RTL flow parameters:

- **Grid & Columns:** Standard phone layouts utilize a 4-column fluid layout with `16px` (`1rem`) lateral margins and gutters. Foldable and tablet screens expand to an 8-column layout with `24px` margins.
- **RTL Directional Anchor:** Flow coordinates start from right: trailing items (e.g., chevrons, timestamps) anchor to the physical left, while primary semantic anchors (checkboxes, avatar badges, timeline nodes) anchor to the physical right.
- **Thumb Reach & Navigation Clearance:** The bottom area features an expanded `96px` safe reserve to accommodate the docked bottom bar and the raised, glowing voice trigger button without occluding scrollable timeline items.

## Elevation & Depth

Visual depth is achieved through **Material 3 surface layering** reinforced by localized, low-opacity color tinting:

- **Base Layer (Surface 0):** Background void `#0B0F19`, flat and absorptive.
- **Elevated Cards (Surface 1):** `#1E2235` paired with a delicate, luminous border: `1px solid rgba(255, 255, 255, 0.05)`.
- **Active Task / Focused State (Surface 2):** `#1F2937` with an ambient glow tinted to the semantic state: `0 8px 24px -4px rgba(99, 102, 241, 0.25)`.
- **Audio Pulse & Recording Overlay:** Radial gradient rings radiating from `#8B5CF6` and `#06B6D4` with multi-tier blur states (12px to 48px blur, opacity scaling between 0.15 and 0.45) mimicking sound energy.
- **Modals & Bottom Sheets:** `#1E2235` backdrop blur filter (`20px`), backed by `80%` surface opacity, crowned with an upward-diffusing shadow `0 -12px 32px rgba(0, 0, 0, 0.6)`.

## Shapes

The design uses balanced, modern radii across interactive and structural surfaces:

- **Large Surfaces & Modals:** Standard task containers and bottom sheets adopt `rounded-xl` (24px) to soften technical density.
- **Cards & Banners:** Primary timeline items, voice summary containers, and settings cards adopt `rounded-lg` (16px).
- **Interactive Controls & Badges:** Voice action chips, Jalali day pickers, and filter pills employ fully continuous pill geometry (`rounded-full` / 9999px).
- **Checkboxes & Form Controls:** `rounded` (8px) for comfortable tap target recognition while retaining structured precision.

## Components

### 1. Floating Voice Action Button (FAB)
- **Geometry & Placement:** 68x68dp circle elevated center-bottom, protruding 24dp above the navigation bar.
- **Resting State:** Linear gradient from `#8B5CF6` to `#6366F1`, elevated by a soft cyan-violet shadow (`0 8px 20px rgba(99, 102, 241, 0.4)`).
- **Active Recording State:** Expands with concentric animated SVG rings; dynamic sine wave oscillates along the perimeter using `#06B6D4`.

### 2. Task Cards & RTL Timeline Nodes
- **Node Position:** Anchored rigidly to the physical right edge. Completed nodes illuminate in `#10B981`; active nodes pulse in `#8B5CF6`.
- **Card Interior:** Physical right side contains the status checkbox and task title (`title-md`). The physical left side houses the Jalali time chip (`14:30` in Persian digits) and priority indicator dot.
- **Card Background:** Surface `#1E2235` with hover/touch reaction transitioning to `#1F2937`.

### 3. Jalali Date Header Chips
- **Container:** Horizontal scroll strip, RTL direction.
- **Dormant Chip:** `#1E2235` surface, neutral text, 12dp horizontal padding, 8dp vertical padding, `rounded-full`.
- **Selected Day:** High-contrast gradient background (`#8B5CF6` to `#06B6D4`), `#FFFFFF` text, faint cyan drop-shadow.

### 4. Voice Quick Action Chips
- **Form:** Pill badges with a leading (right-aligned) micro-icon (e.g., microphone wave, sparkles).
- **Border:** Subtle translucent border (`1px solid rgba(139, 92, 246, 0.3)`).
- **Interaction:** Haptic micro-tick on release, prompting conversational voice confirmation.

### 5. Input Fields & Search Bars
- **Style:** Deep container `#111827` inset with a `1px` border of `#374151`.
- **Focus State:** Border transitions to `#06B6D4` with an inner ambient glow of `rgba(6, 182, 212, 0.15)`.
- **RTL Trailing Icons:** Clear and speech-to-text triggers align to the physical left edge; search icon anchors to the right.

### 6. Bottom Navigation Bar
- **Architecture:** Material 3 asymmetrical docked bar with a central cutout curve for the floating microphone.
- **Surface:** `#111827` with 90% opacity and an active background blur of 16px.
- **Active Indicator:** Smooth pill container behind active icon highlighted with `#1E2235` and `#8B5CF6` icon tint.