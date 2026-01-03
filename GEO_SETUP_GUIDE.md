# Setup Guide - Phase 9: Geolocation & Flags 🌍

## Manual steps you must perform:

### 1. Download the MaxMind Database (GeoLite2-Country.mmdb)

**Steps:**
1. Visit: https://www.maxmind.com/en/geolite2-free-geolocation-database
2. Create an account (free) or sign in
3. Download the **GeoLite2 Country** file (`.mmdb`)
4. Create the folder: `netBoundStar-view/src/main/resources/geo/`
5. Place the file there with the exact name: `GeoLite2-Country.mmdb`

**Result:**
```
netBoundStar-view/src/main/resources/geo/GeoLite2-Country.mmdb
```

### 2. Download Flag Icons

**Steps:**
1. Download a package of flag icons (recommended):
   - GitHub: https://github.com/lipis/flag-icons (SVG or PNG versions)
   - Or search for "Flag Icons ISO 3166" on the web
2. You need icons named with **2 letters** (uppercase or lowercase):
   - `br.svg` or `BR.svg` or `br.png` or `BR.png` (Brazil)
   - `us.svg` or `US.svg` or `us.png` or `US.png` (USA)
   - `de.svg` or `DE.svg` or `de.png` or `DE.png` (Germany)
   - `fr.svg` or `FR.svg` or `fr.png` or `FR.png` (France)
   - `jp.svg` or `JP.svg` or `jp.png` or `JP.png` (Japan)
   - ...etc
3. **Format**: SVG (recommended) or PNG
4. **Recommended size**: For PNG: 24x24px or 32x32px
5. **Case**: Case does not matter (the system tries both)
6. Create the folder: `netBoundStar-view/src/main/resources/flags/`
7. Put all files there

**Result (example with lowercase names):**
```
netBoundStar-view/src/main/resources/flags/
├── br.svg
├── us.svg
├── de.svg
├── fr.svg
├── jp.svg
├── ru.svg
└── ... (as many as you want)
```

**OR with PNG:**
```
netBoundStar-view/src/main/resources/flags/
├── br.png
├── us.png
├── de.png
├── fr.png
├── jp.png
├── ru.png
└── ...
```

**OR mixed:**
```
netBoundStar-view/src/main/resources/flags/
├── br.svg
├── US.png
├── de.svg
├── FR.svg
├── jp.png
└── ...
```

## Expected Final Structure:

```
netBoundStar-view/src/main/resources/
├── geo/
│   └── GeoLite2-Country.mmdb (optional, but recommended)
└── flags/
    ├── BR.svg (or BR.png)
    ├── US.svg (or US.png)
    ├── DE.svg (or DE.png)
    ├── FR.svg (or FR.png)
    ├── CN.svg (or CN.png)
    ├── AU.svg (or AU.png)
    └── ... (more countries)
```

## How the system works:

1. **On startup**, `GeoService` loads the `.mmdb` file into memory
2. **For each remote IP**, the system:
   - Resolves the hostname via DNS
   - Resolves the country via GeoIP (MaxMind)
   - Loads the corresponding flag from the cache
3. **On the canvas**, instead of a white dot, it draws the flag
4. **If it cannot find** the flag or the database, it falls back to a white dot (no error)

## Verification:

After placing the files, when you run the application:
- Check the console for: `✓ GeoLite2 loaded successfully!`
- If you see `⚠ WARNING: GeoLite2-Country.mmdb file not found`, put the file in the correct folder
- Flags will appear automatically as IPs are resolved

## Tips:

- You do not need ALL countries - add whichever you want
- Common flags: us, br, de, fr, gb, jp, ru, cn, au, ca
- **SVG is preferable to PNG** (smaller size, better quality at any resolution)
- **Case does not matter**: `br.svg`, `BR.svg`, `Br.svg` - all work!
- The system tries SVG first, then PNG
- The system tries uppercase first, then lowercase
- If a country has no flag, it shows a white dot (no error)
- The in-memory cache avoids loading the same image multiple times
- You can mix formats: `br.svg`, `US.png`, `de.svg` (all together work!)

Good luck! 🌍🚀
