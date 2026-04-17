from pathlib import Path
import psycopg2


DB_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "dbname": "roma_events",
    "user": "postgres",
    "password": "postgres"
}


def run_sql_file(cursor, file_path: Path):
    sql = file_path.read_text(encoding="utf-8")
    cursor.execute(sql)
    print(f"[OK] Eseguito: {file_path.name}")


def main():
    base_dir = Path(__file__).parent
    schema_file = base_dir / "schema.sql"
    seed_file = base_dir / "seed.sql"

    conn = None
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        conn.autocommit = False

        with conn.cursor() as cur:
            run_sql_file(cur, schema_file)

            if seed_file.exists():
                run_sql_file(cur, seed_file)

        conn.commit()
        print("[OK] Database inizializzato correttamente.")

    except Exception as e:
        if conn:
            conn.rollback()
        print(f"[ERRORE] {e}")

    finally:
        if conn:
            conn.close()


if __name__ == "__main__":
    main()