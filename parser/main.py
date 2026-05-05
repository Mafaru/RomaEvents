import json
from scraper import parse_event_list, parse_event_detail
from normalizer import normalize_raw_date_text
from importer import import_items

MAX_PAGES = 10


def main():
    events = parse_event_list(max_pages=MAX_PAGES)
    print(f"[INFO] Totale eventi trovati in lista: {len(events)}")

    enriched = []
    for idx, event in enumerate(events, start=1):
        print(f"[{idx}/{len(events)}] Leggo dettaglio: {event['title']}")
        detail = parse_event_detail(event["detail_url"])
        normalized = normalize_raw_date_text(detail["raw_date_text"], default_year=2026)

        enriched.append({
            "title": detail["title"],
            "detail_url": detail["detail_url"],
            "theme_text": detail["theme_text"],
            "raw_date_text": detail["raw_date_text"],
            "description": detail["description"],
            "location_text": detail["location_text"],
            "popup_event_text": detail["popup_event_text"],
            "coordinates": detail["coordinates"],
            "normalized_dates": normalized,
        })

    with open("sample_output.json", "w", encoding="utf-8") as f:
        json.dump(enriched, f, ensure_ascii=False, indent=2)

    print("[OK] Salvato sample_output.json")

    import_items(enriched)


if __name__ == "__main__":
    main()