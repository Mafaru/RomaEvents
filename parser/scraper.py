import re
import html
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin

BASE_URL = "https://www.comune.roma.it"
EVENTS_URL = "https://www.comune.roma.it/web/it/eventi.page"

HEADERS = {
    "User-Agent": "Mozilla/5.0"
}


def clean_text(text: str | None) -> str | None:
    if not text:
        return None

    text = html.unescape(text)
    text = text.replace("\xa0", " ")
    text = " ".join(text.split()).strip()
    return text


def fetch_html(url: str) -> str:
    response = requests.get(url, headers=HEADERS, timeout=20)
    response.raise_for_status()
    return response.text


def build_page_url(page_num: int) -> str:
    if page_num == 1:
        return EVENTS_URL
    return f"{EVENTS_URL}?pagina={page_num}"


def parse_event_list(max_pages: int = 3) -> list[dict]:
    events = []
    seen_urls = set()

    for page_num in range(1, max_pages + 1):
        url = build_page_url(page_num)
        print(f"[INFO] Leggo lista eventi pagina {page_num}: {url}")

        try:
            html_text = fetch_html(url)
        except Exception as e:
            print(f"[WARN] Impossibile leggere la pagina {page_num}: {e}")
            continue

        soup = BeautifulSoup(html_text, "lxml")
        page_events_found = 0

        for a in soup.find_all("a", href=True):
            href = a.get("href")
            if not href or "/web/it/evento/" not in href:
                continue

            detail_url = urljoin(BASE_URL, href)
            if detail_url in seen_urls:
                continue
            seen_urls.add(detail_url)

            title = clean_text(a.get_text())
            if not title:
                continue

            events.append({
                "title": title,
                "detail_url": detail_url,
            })
            page_events_found += 1

        print(f"[INFO] Pagina {page_num}: trovati {page_events_found} eventi nuovi")

    return events


def extract_main_text(soup: BeautifulSoup) -> str:
    text = clean_text(soup.get_text(" ", strip=True)) or ""

    start_markers = [
        "Home Attualità Notizie ed eventi Tutti gli eventi",
        "Tutti gli eventi"
    ]
    for marker in start_markers:
        idx = text.find(marker)
        if idx != -1:
            text = text[idx:]
            break

    end_markers = [
        "Legenda",
        "Condividi Facebook",
        "Ti è stata utile questa pagina?",
        "Roma Capitale Contatti"
    ]
    cut_positions = [text.find(marker) for marker in end_markers if text.find(marker) != -1]
    if cut_positions:
        text = text[:min(cut_positions)]

    return clean_text(text) or ""


def extract_title(main_text: str) -> str | None:
    marker = "Tutti gli eventi"
    idx = main_text.find(marker)
    if idx == -1:
        return None

    tail = main_text[idx + len(marker):].strip()
    tematica_idx = tail.find("Tematica:")
    if tematica_idx != -1:
        return clean_text(tail[:tematica_idx].strip())

    return None


def extract_theme_and_date(main_text: str) -> tuple[str | None, str | None]:
    match = re.search(
        r"Tematica:\s*(.+?)\s+((?:\d{1,2}.*?))(?:\s+[A-ZÀ-Ú][a-zà-ú]|$)",
        main_text
    )
    if match:
        return clean_text(match.group(1)), clean_text(match.group(2))

    fallback = re.search(r"Tematica:\s*(.+?)\s+(\d{1,2}.*)", main_text)
    if fallback:
        return clean_text(fallback.group(1)), clean_text(fallback.group(2))

    return None, None


def clean_raw_date_text(raw_date_text: str | None) -> str | None:
    if not raw_date_text:
        return None

    value = html.unescape(raw_date_text.strip())
    value = re.sub(r"\s*©.*$", "", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value


def extract_description(main_text: str, theme_text: str | None, raw_date_text: str | None) -> str | None:
    text = main_text

    if "Tematica:" in text:
        text = text.split("Tematica:", 1)[1].strip()

    if theme_text and text.startswith(theme_text):
        text = text[len(theme_text):].strip()

    if raw_date_text and text.startswith(raw_date_text):
        text = text[len(raw_date_text):].strip()

    text = re.sub(r"\bRED\s*$", "", text).strip()
    text = re.sub(r"\s+", " ", text).strip()

    return clean_text(text)


def extract_map_info_from_scripts(soup: BeautifulSoup) -> dict:
    scripts = []
    for script in soup.find_all("script"):
        txt = script.string or script.get_text(" ", strip=False)
        if txt:
            scripts.append(txt)

    all_scripts = "\n".join(scripts)

    lat_match = re.search(r"var\s+markerLat\s*=\s*([-+]?\d+\.\d+)", all_scripts)
    lon_match = re.search(r"var\s+markerLon\s*=\s*([-+]?\d+\.\d+)", all_scripts)

    latitude = float(lat_match.group(1)) if lat_match else None
    longitude = float(lon_match.group(1)) if lon_match else None

    text_match = re.search(
        r"text:\s*'<strong>(.*?)</strong>'",
        all_scripts,
        flags=re.DOTALL
    )
    location_text = clean_text(text_match.group(1)) if text_match else None

    descr_match = re.search(
        r"descr:\s*'(.*?)'",
        all_scripts,
        flags=re.DOTALL
    )
    popup_event_text = clean_text(descr_match.group(1)) if descr_match else None

    if latitude is None or longitude is None:
        latlng_match = re.search(
            r"latLng:\s*\[\s*([-+]?\d+\.\d+)\s*,\s*([-+]?\d+\.\d+)\s*\]",
            all_scripts
        )
        if latlng_match:
            latitude = float(latlng_match.group(1))
            longitude = float(latlng_match.group(2))

    return {
        "location_text": location_text,
        "popup_event_text": popup_event_text,
        "coordinates": {
            "latitude": latitude,
            "longitude": longitude
        } if latitude is not None and longitude is not None else None
    }


def parse_event_detail(detail_url: str) -> dict:
    html_text = fetch_html(detail_url)
    soup = BeautifulSoup(html_text, "lxml")

    main_text = extract_main_text(soup)
    title = extract_title(main_text)
    theme_text, raw_date_text = extract_theme_and_date(main_text)
    raw_date_text = clean_raw_date_text(raw_date_text)
    description = extract_description(main_text, theme_text, raw_date_text)
    map_info = extract_map_info_from_scripts(soup)

    return {
        "title": title,
        "theme_text": theme_text,
        "raw_date_text": raw_date_text,
        "description": description,
        "location_text": map_info["location_text"],
        "popup_event_text": map_info["popup_event_text"],
        "coordinates": map_info["coordinates"],
        "detail_url": detail_url,
        "raw_text": main_text
    }