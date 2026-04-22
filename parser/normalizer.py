import re
from datetime import date

MONTHS_IT = {
    "gennaio": 1,
    "febbraio": 2,
    "marzo": 3,
    "aprile": 4,
    "maggio": 5,
    "giugno": 6,
    "luglio": 7,
    "agosto": 8,
    "settembre": 9,
    "ottobre": 10,
    "novembre": 11,
    "dicembre": 12,
}


def clean_value(value: str | None) -> str | None:
    if not value:
        return None
    value = value.strip().lower()
    value = re.sub(r"\s+", " ", value)
    return value


def parse_single_date(text: str, default_year: int) -> list[dict]:
    match = re.fullmatch(r"(\d{1,2})\s+([a-zà-ù]+)", text)
    if not match:
        return []

    day = int(match.group(1))
    month_name = match.group(2)
    month = MONTHS_IT.get(month_name)
    if not month:
        return []

    return [{
        "start_date": date(default_year, month, day).isoformat(),
        "end_date": None,
        "type": "single"
    }]


def parse_same_month_range(text: str, default_year: int) -> list[dict]:
    match = re.fullmatch(r"(\d{1,2})\s*-\s*(\d{1,2})\s+([a-zà-ù]+)", text)
    if not match:
        return []

    day_start = int(match.group(1))
    day_end = int(match.group(2))
    month_name = match.group(3)
    month = MONTHS_IT.get(month_name)
    if not month:
        return []

    return [{
        "start_date": date(default_year, month, day_start).isoformat(),
        "end_date": date(default_year, month, day_end).isoformat(),
        "type": "range"
    }]


def parse_cross_month_range(text: str, default_year: int) -> list[dict]:
    match = re.fullmatch(
        r"(\d{1,2})\s+([a-zà-ù]+)\s*-\s*(\d{1,2})\s+([a-zà-ù]+)",
        text
    )
    if not match:
        return []

    day_start = int(match.group(1))
    month_start_name = match.group(2)
    day_end = int(match.group(3))
    month_end_name = match.group(4)

    month_start = MONTHS_IT.get(month_start_name)
    month_end = MONTHS_IT.get(month_end_name)
    if not month_start or not month_end:
        return []

    return [{
        "start_date": date(default_year, month_start, day_start).isoformat(),
        "end_date": date(default_year, month_end, day_end).isoformat(),
        "type": "range"
    }]


def parse_multiple_days_same_month(text: str, default_year: int) -> list[dict]:
    # esempio: "17, 19 e 20 aprile"
    match = re.fullmatch(r"([\d,\se]+)\s+([a-zà-ù]+)", text)
    if not match:
        return []

    days_part = match.group(1)
    month_name = match.group(2)
    month = MONTHS_IT.get(month_name)
    if not month:
        return []

    numbers = re.findall(r"\d{1,2}", days_part)
    if len(numbers) < 2:
        return []

    occurrences = []
    for n in numbers:
        day = int(n)
        occurrences.append({
            "start_date": date(default_year, month, day).isoformat(),
            "end_date": None,
            "type": "single"
        })

    return occurrences


def parse_multiple_full_dates(text: str, default_year: int) -> list[dict]:
    # esempio: "22 aprile e 19 maggio"
    matches = re.findall(r"(\d{1,2})\s+([a-zà-ù]+)", text)
    if len(matches) < 2:
        return []

    occurrences = []
    for day_str, month_name in matches:
        month = MONTHS_IT.get(month_name)
        if not month:
            return []

        occurrences.append({
            "start_date": date(default_year, month, int(day_str)).isoformat(),
            "end_date": None,
            "type": "single"
        })

    return occurrences


def normalize_raw_date_text(raw_date_text: str | None, default_year: int = 2026) -> dict:
    original = raw_date_text
    text = clean_value(raw_date_text)

    if not text:
        return {
            "raw_date_text": original,
            "occurrences": [],
            "matched_pattern": None
        }

    parsers = [
        ("multiple_full_dates", parse_multiple_full_dates),
        ("multiple_days_same_month", parse_multiple_days_same_month),
        ("cross_month_range", parse_cross_month_range),
        ("same_month_range", parse_same_month_range),
        ("single_date", parse_single_date),
    ]

    for pattern_name, parser in parsers:
        result = parser(text, default_year)
        if result:
            return {
                "raw_date_text": original,
                "occurrences": result,
                "matched_pattern": pattern_name
            }

    return {
        "raw_date_text": original,
        "occurrences": [],
        "matched_pattern": "unparsed"
    }