import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def expected_catalog():
    payload = json.loads((ROOT / "ml_service/class_map.json").read_text(encoding="utf-8"))
    rows = [
        (row["result_id"], row["name_en"], row["name_ko"])
        for row in payload["classes"]
    ]
    unknown = payload["unknown"]
    rows.append((unknown["result_id"], unknown["name_en"], unknown["name_ko"]))
    return rows


def test_local_seed_and_oracle_migration_match_the_ml_class_map():
    local_sql = (ROOT / "src/main/resources/db/local-data.sql").read_text(encoding="utf-8")
    migration_sql = (
        ROOT / "docs/db/oracle-traffic-sign-result-catalog.sql"
    ).read_text(encoding="utf-8")
    local_rows = [
        (int(no), name, content)
        for no, name, content in re.findall(
            r"INSERT INTO RESULT_IMAGE .*? VALUES \(([0-9]+), '([^']*)', '([^']*)', 'none'\);",
            local_sql,
        )
    ]
    migration_rows = [
        (int(no), name, content)
        for no, name, content in re.findall(
            r"upsert_result\(([0-9]+), '([^']*)', '([^']*)'\);",
            migration_sql,
        )
    ]

    assert local_rows == expected_catalog()
    assert migration_rows == expected_catalog()
