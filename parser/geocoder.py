import requests
from time import sleep

HEADERS = {
    "User-Agent": "RomaEventsParser/1.0 (student project)"
}


def geocode_place(query: str | None) -> dict | None:
    if not query:
        return None

    params = {
        "q": query,
        "format": "jsonv2",
        "limit": 1
    }

    response = requests.get(
        "https://nominatim.openstreetmap.org/search",
        params=params,
        headers=HEADERS,
        timeout=20
    )
    response.raise_for_status()

    results = response.json()
    if not results:
        return None

    best = results[0]
    return {
        "query": query,
        "latitude": float(best["lat"]),
        "longitude": float(best["lon"]),
        "display_name": best.get("display_name")
    }


def geocode_location_text(location_text: str | None) -> dict | None:
    if not location_text:
        return None

    candidates = [
        f"{location_text}, Roma",
        location_text
    ]

    for query in candidates:
        result = geocode_place(query)
        sleep(1)
        if result:
            return result

    return None