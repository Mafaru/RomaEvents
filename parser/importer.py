from __future__ import annotations

import psycopg2
from psycopg2.extras import RealDictCursor


DB_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "dbname": "roma_events",
    "user": "postgres",
    "password": "postgres",
}


def map_theme_to_category_name(theme_text: str | None) -> str:
    if not theme_text:
        return "Cultura"

    t = theme_text.lower()

    if "sport" in t:
        return "Sport"
    if "turismo" in t:
        return "Turismo"
    if "ambiente" in t:
        return "Ambiente"
    if "mobilità" in t or "trasporti" in t:
        return "Mobilità e Trasporti"
    if "scuola" in t:
        return "Scuola"
    if "sociale" in t:
        return "Sociale"
    if "innovazione" in t or "smart city" in t:
        return "Innovazione e Smart City"
    if "diritti" in t:
        return "Diritti"
    if "patrimonio" in t:
        return "Patrimonio"
    if "pari opportunità" in t:
        return "Pari opportunità"
    if "disabilità" in t:
        return "Disabilità"
    if "formazione" in t or "lavoro" in t:
        return "Formazione e Lavoro"
    if "urbanistica" in t:
        return "Urbanistica"
    if t.strip() == "roma":
        return "Roma"

    # Temi artistico-culturali: con il seed attuale confluiscono in Cultura
    if (
        "musica" in t
        or "teatro" in t
        or "danza" in t
        or "cinema" in t
        or "mostre" in t
        or "fotografia" in t
        or "cultura" in t
        or "eventi ad ingresso libero" in t
        or "iniziative ed eventi" in t
        or "giovani" in t
        or "bioparco" in t
    ):
        return "Cultura"

    return "Cultura"


def get_connection():
    return psycopg2.connect(**DB_CONFIG)


def get_category_id(cur, category_name: str) -> int:
    cur.execute(
        """
        SELECT id
        FROM categories
        WHERE name = %s
        """,
        (category_name,)
    )
    row = cur.fetchone()

    if row:
        return row["id"]

    cur.execute(
        """
        INSERT INTO categories (name, description)
        VALUES (%s, %s)
        RETURNING id
        """,
        (category_name, f"Categoria creata automaticamente dal parser: {category_name}")
    )
    return cur.fetchone()["id"]


def find_existing_event_id(cur, source_url: str | None, title: str) -> int | None:
    if source_url:
        cur.execute(
            """
            SELECT id
            FROM events
            WHERE source_url = %s
            """,
            (source_url,)
        )
        row = cur.fetchone()
        if row:
            return row["id"]

    cur.execute(
        """
        SELECT id
        FROM events
        WHERE title = %s
        ORDER BY id DESC
        LIMIT 1
        """,
        (title,)
    )
    row = cur.fetchone()
    return row["id"] if row else None


def upsert_event(cur, item: dict) -> int:
    category_name = map_theme_to_category_name(item.get("theme_text"))
    category_id = get_category_id(cur, category_name)

    title = item.get("title")
    description = item.get("description")
    raw_date_text = item.get("raw_date_text")
    address = item.get("location_text")
    source_url = item.get("detail_url")

    coordinates = item.get("coordinates") or {}
    latitude = coordinates.get("latitude")
    longitude = coordinates.get("longitude")

    existing_id = find_existing_event_id(cur, source_url, title)

    if existing_id is not None:
        cur.execute(
            """
            UPDATE events
            SET
                title = %s,
                description = %s,
                raw_date_text = %s,
                address = %s,
                latitude = %s,
                longitude = %s,
                category_id = %s,
                source_url = %s
            WHERE id = %s
            """,
            (
                title,
                description,
                raw_date_text,
                address,
                latitude,
                longitude,
                category_id,
                source_url,
                existing_id
            )
        )
        return existing_id

    cur.execute(
        """
        INSERT INTO events (
            title,
            description,
            raw_date_text,
            address,
            latitude,
            longitude,
            category_id,
            source_url
        )
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        RETURNING id
        """,
        (
            title,
            description,
            raw_date_text,
            address,
            latitude,
            longitude,
            category_id,
            source_url
        )
    )
    return cur.fetchone()["id"]


def replace_event_occurrences(cur, event_id: int, item: dict) -> None:
    cur.execute(
        """
        DELETE FROM event_occurrences
        WHERE event_id = %s
        """,
        (event_id,)
    )

    normalized = item.get("normalized_dates") or {}
    occurrences = normalized.get("occurrences") or []

    for occ in occurrences:
        start_date = occ.get("start_date")
        end_date = occ.get("end_date")

        if not start_date:
            continue

        start_datetime = f"{start_date} 00:00:00"
        end_datetime = f"{end_date} 23:59:59" if end_date else None

        cur.execute(
            """
            INSERT INTO event_occurrences (event_id, start_datetime, end_datetime)
            VALUES (%s, %s, %s)
            """,
            (event_id, start_datetime, end_datetime)
        )


def import_items(items: list[dict]) -> None:
    conn = None
    try:
        conn = get_connection()
        conn.autocommit = False

        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            for item in items:
                if not item.get("title"):
                    continue

                event_id = upsert_event(cur, item)
                replace_event_occurrences(cur, event_id, item)

        conn.commit()
        print("[OK] Import completato con successo.")

    except Exception as e:
        if conn:
            conn.rollback()
        print(f"[ERRORE IMPORT] {e}")
        raise

    finally:
        if conn:
            conn.close()