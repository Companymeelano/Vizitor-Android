#!/usr/bin/env python3
"""
Guard for the "no guessed identifiers" rule.

Every SQL statement written in the Android data layer is checked against
docs/schema/meelano-columns.tsv, which was extracted from the live server audit
output. Any column that does not exist on the real database is reported - so a
typo or an invented column can never reach the app.

NOTE: this is a PYTHON tool. Never open it and never run it inside SSMS - T-SQL
would answer with "Msg 137 @echo", "Msg 911 Database 'REM' does not exist" and
a wall of syntax errors. In SSMS you only ever run the .sql files in sql/.

Usage:
    python3 check_sql_columns.py                 # scans ../*.kt and ../*.sql
    python3 check_sql_columns.py file1 file2 ...  # scans specific files

What it understands:
    FROM dbo.inventory i          -> alias i = dbo.inventory
    JOIN dbo.anbars a ON ...      -> alias a = dbo.anbars
    i.shka  /  dbo.inventory.shka -> validated against the table's column list
Columns that are unqualified are only checked when the query touches exactly one
table (otherwise they are skipped, not guessed).
"""
import re
import sys
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
TSV = ROOT / "docs" / "schema" / "meelano-columns.tsv"
VALUES_TSV = ROOT / "docs" / "schema" / "meelano-values.tsv"

# identifiers that are not columns: aliases, table names, SQL functions, keywords
IGNORE_QUALIFIERS = {"sys", "dbo", "Hamrah", "warehousing", "security", "EMS", "kg", "information_schema"}
# catalogues that appear in FROM/JOIN but are not part of the ERP schema:
# a statement that mixes one ERP table with sys.columns is NOT a single-table query
SYSTEM_SCHEMAS = {"sys", "information_schema", "tempdb"}


def load_schema():
    tables = {}
    if not TSV.exists():
        sys.exit(f"schema file not found: {TSV}")
    for line in TSV.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        name, cols = line.split("\t", 1)
        tables[name.strip().lower()] = {c.strip() for c in cols.split(",") if c.strip()}
    return tables


def load_values():
    """Verified constant values (e.g. the char(1) active flag is 't', not '1')."""
    values = {}
    if VALUES_TSV.exists():
        for line in VALUES_TSV.read_text(encoding="utf-8").splitlines():
            if not line.strip() or line.startswith("#"):
                continue
            parts = line.split("\t")
            if len(parts) >= 2:
                values[parts[0].strip()] = parts[1].strip()
    return values


def check_declared_constants(text: str, path_name: str):
    """Every Kotlin constant that claims a verified value is checked against the
    database:   const val NAME = "t"   // value: dbo.inventory.active"""
    problems = []
    checked = 0
    for m in re.finditer(r'const\s+val\s+(\w+)\s*=\s*("([^"]*)"|\d+)\s*//\s*value:\s*([\w.]+)', text):
        name, literal, key = m.group(1), m.group(3) if m.group(3) is not None else m.group(2), m.group(4)
        if key not in VALUES:
            problems.append(f"constant {name} claims an unknown verified value key '{key}'")
            continue
        expected = VALUES[key]
        if str(literal) != expected:
            problems.append(f"constant {name} = \"{literal}\" but the database value for "
                            f"{key} is \"{expected}\"")
        checked += 1
    if checked:
        print(f"  {path_name:32s} {checked} declared constant(s) match the database")
    return problems


def kotlin_sql_strings(text: str):
    """SQL inside Kotlin string literals (raw and escaped)."""
    out = []
    out += re.findall(r'"""(.*?)"""', text, flags=re.S)
    for m in re.finditer(r'"((?:[^"\\\n]|\\.)*)"', text):
        s = m.group(1)
        if re.search(r"\b(SELECT|INSERT|UPDATE|DELETE|EXEC)\b", s, re.I):
            out.append(s)
    return out


def sql_files(text: str):
    """SQL statement blocks of a .sql file (up to ; or GO).

    Comments must be stripped before this runs, and the full match is returned:
    an earlier version returned only the keyword and then a crude
    `[^;]{0,4000}` regex grabbed comment text along with the statement, which
    both hid real statements and invented columns from prose."""
    return [m.group(0) for m in re.finditer(
        r"(?:SELECT|INSERT|UPDATE|DELETE)[^;]*?(?:;|\n\s*GO\b)", text, flags=re.S | re.I)]


NOT_ALIAS = ("ON", "WHERE", "ORDER", "GROUP", "HAVING", "INNER", "LEFT", "RIGHT",
             "FULL", "CROSS", "JOIN", "WITH", "AS", "SET", "VALUES")


def alias_map(sql: str):
    """alias/table -> real table name, from FROM/JOIN clauses and DML targets.

    The DML targets matter: an app INSERT names its columns in a bare list
    (`INSERT INTO dbo.subsailtemp_pish (mod, shfacfo, ...) VALUES (...)`), and
    those names are exactly the ones that must never be guessed - so the table
    of an INSERT / UPDATE / DELETE is mapped too, and its columns are validated.
    """
    amap = {}

    def add(raw: str, alias=None):
        table = raw.replace("[", "").replace("]", "")
        key = table.lower()
        if key.split(".")[0] in SYSTEM_SCHEMAS:
            amap[table.lower()] = key          # counted, but never column-validated
            if alias and alias.upper() not in NOT_ALIAS:
                amap[alias.lower()] = key
            return
        if key not in SCHEMA:
            # accept both spellings: the schema file may hold "dbo.sailfact_pish"
            # while the SQL says "sailfact_pish", or the other way round.
            short_key = key.split(".")[-1]
            if short_key in SCHEMA:
                key = short_key
            elif f"dbo.{short_key}" in SCHEMA:
                key = f"dbo.{short_key}"
            else:
                return
        amap[table.lower()] = key
        short = key.split(".")[-1]                    # also accept "inventory.col"
        amap.setdefault(short, key)
        if alias and alias.upper() not in NOT_ALIAS:
            amap[alias.lower()] = key

    for m in re.finditer(
            r"\b(?:FROM|JOIN)\s+((?:\[?\w+\]?\.)?\[?\w+\]?)(?:\s+(?:AS\s+)?(\w+))?", sql, flags=re.I):
        add(m.group(1), m.group(2))

    for m in re.finditer(
            r"\b(?:INSERT\s+INTO|UPDATE|DELETE\s+FROM)\s+((?:\[?\w+\]?\.)?\[?\w+\]?)",
            sql, flags=re.I):
        add(m.group(1))

    return amap


SQL_KEYWORDS = {
    "select", "from", "where", "and", "or", "order", "by", "group", "having", "as",
    "inner", "left", "right", "full", "outer", "cross", "join", "on", "desc", "asc",
    "top", "distinct", "is", "not", "null", "case", "when", "then", "else", "end",
    "in", "like", "between", "union", "all", "exists", "with", "offset", "rows",
    "fetch", "next", "only", "over", "partition", "cast", "convert", "collate",
    "values", "insert", "into", "update", "delete", "set", "exec", "execute",
    "declare", "if", "begin", "return", "int", "bigint", "smallint", "tinyint",
    "money", "decimal", "numeric", "nvarchar", "varchar", "nchar", "char", "bit",
    "date", "datetime", "float", "real", "text", "ntext", "sysname", "max",
    "true", "false",
}

SQL_FUNCTIONS = {
    "count", "sum", "min", "max", "avg", "len", "isnull", "coalesce", "substring",
    "round", "getdate", "pwdcompare", "db_name", "serverproperty", "left", "right",
    "cast", "convert", "charindex", "stuff", "replicate", "upper", "lower", "ltrim",
    "rtrim", "replace", "abs", "floor", "ceiling", "datediff", "dateadd", "convert",
    "row_number", "rank", "dense_rank", "string_agg", "concat", "iif", "try_cast",
    "try_convert", "format", "object_id", "schema_name", "scope_identity", "error_message",
    "datalength", "len", "isnumeric", "quotename", "replicate", "space", "translate",
    "string_split", "rowcount_big", "checksum", "hashbytes", "newid", "suser_sname",
    "user_name", "suser_sid", "is_srvrolemember", "has_perms_by_name", "sysdatetime",
    "getutcdate", "datepart", "datename", "eomonth", "choose", "parse", "trim",
}


def _ci(columns):
    return {c.lower(): c for c in columns}


def strip_literals(sql: str) -> str:
    """Replace every string literal - with or without the N prefix - by a space.

    The old regex `'[^']*'` left the N of N'...' behind, and that stray N was
    then reported as 'unknown unqualified column N'."""
    out, i, n = [], 0, len(sql)
    while i < n:
        c = sql[i]
        if c == "'" or (c in "Nn" and sql[i + 1:i + 2] == "'"):
            j = i + 2 if c != "'" else i + 1
            while j < n:
                if sql[j] == "'":
                    if sql[j + 1:j + 2] == "'":
                        j += 2
                        continue
                    j += 1
                    break
                j += 1
            out.append(" ")
            i = j
        else:
            out.append(c)
            i += 1
    return "".join(out)


def check_statement(sql: str, amap: dict):
    problems = []
    known_tables = set(amap.values())
    single = next(iter(known_tables)) if len(known_tables) == 1 else None
    if single is not None and single not in SCHEMA:
        single = None                     # the statement only touches a catalogue

    # output aliases are not columns - both spellings are used in these scripts:
    #   SELECT name AS alias  and  SELECT alias = expression
    output_aliases = {m.group(1).lower() for m in re.finditer(r"\bAS\s+(\w+)", sql, flags=re.I)}
    # `SELECT x = ...` and the comma-list after it are aliases - but the same shape
    # appears in `UPDATE t SET a = 1, b = 2`, where the names ARE columns. Gating
    # this on SELECT is what makes the guard catch a typo in an UPDATE.
    if re.search(r"\bSELECT\b", sql, flags=re.I):
        output_aliases |= {m.group(1).lower() for m in re.finditer(
            r"(?:\bSELECT\b|,)\s*(\w+)\s*=", sql, flags=re.I)}
    # Kotlin string interpolation names inside the SQL text
    interpolation = {m.group(1).lower() for m in re.finditer(r"\$\{?(\w+)", sql)}

    # qualified references: alias.column / table.column
    for m in re.finditer(r"\b([A-Za-z_]\w*)\s*\.\s*(\$?\{?\w+\}?)", sql):
        qualifier, column = m.group(1).lower(), m.group(2).lower()
        if qualifier in IGNORE_QUALIFIERS or qualifier in SQL_FUNCTIONS:
            continue
        table = amap.get(qualifier)
        if table is None:
            continue
        if SCHEMA.get(table) is None:
            continue                      # system catalogue: nothing to validate
        if column.startswith("$") or column in interpolation:
            continue                      # built by Kotlin: validated at the call site
        if column.startswith("$"):
            continue
        if column not in _ci(SCHEMA.get(table, [])):
            problems.append(f"unknown column {qualifier}.{column}  (table {table})")

    # unqualified names, only when exactly one table is involved
    if single:
        body = strip_literals(sql)
        body = re.sub(r"@\w+", " ", body)
        body = re.sub(r"[+\-*/=<>(),;]", " ", body)
        # a name that is a table in this statement is not a column (even when the
        # table itself is not in the schema file, e.g. dbo.overal_setting)
        referenced = {m.group(1).strip("[]").lower() for m in re.finditer(
            r"\b(?:FROM|JOIN)\s+(?:\w+\s*\.\s*)?(\[?\w+\]?)", sql, flags=re.I)}
        # a name used as a call is a function, not a column - this also covers
        # the ERP's own functions called as dbo.IsAccountingSystemStarted()
        called = {m.group(1).lower() for m in re.finditer(r"\b([A-Za-z_]\w*)\s*\(", sql)}
        for token in re.findall(r"\b([A-Za-z_]\w*)\b", body):
            low = token.lower()
            if low in SQL_KEYWORDS or low in SQL_FUNCTIONS or low in IGNORE_QUALIFIERS:
                continue
            if low in referenced or low in called:
                continue
            if low in output_aliases or low in interpolation:
                continue
            if low in SCHEMA or low in known_tables or low in amap:
                continue
            # short table name used as a qualifier (forosh_price.shka)
            if low in {t.split(".")[-1] for t in known_tables}:
                continue
            # any word that is used as a qualifier somewhere in this statement
            if re.search(r"\b" + re.escape(token) + r"\s*\.", sql):
                continue
            if token.lower() in _ci(SCHEMA[single]):
                continue
            problems.append(f"unknown unqualified column '{token}'  (single table {single})")
    return problems


def strip_sql_comments(sql: str) -> str:
    """Remove comments, but never inside a string literal.

    Real case: a print line containing '=== sections reported ===' is fine, but
    the same line built with '---' separators made a plain regex eat the rest of
    the line and report a phantom column named 'N'."""
    out, i, n = [], 0, len(sql)
    while i < n:
        c = sql[i]
        if c == "'" or (c in "Nn" and sql[i + 1:i + 2] == "'"):
            j = i + 2 if c != "'" else i + 1
            while j < n:
                if sql[j] == "'":
                    if sql[j + 1:j + 2] == "'":
                        j += 2
                        continue
                    j += 1
                    break
                j += 1
            out.append(sql[i:j])
            i = j
        elif sql.startswith("/*", i):
            j = sql.find("*/", i + 2)
            j = n if j == -1 else j + 2
            out.append("".join(ch if ch == "\n" else " " for ch in sql[i:j]))
            i = j
        elif sql.startswith("--", i):
            j = sql.find("\n", i)
            j = n if j == -1 else j
            out.append(" " * (j - i))
            i = j
        else:
            out.append(c)
            i += 1
    return "".join(out)


def check(path: pathlib.Path):
    text = path.read_text(encoding="utf-8", errors="replace")
    if path.suffix == ".kt":
        statements = kotlin_sql_strings(text)
    elif path.suffix == ".sql":
        statements = sql_files(strip_sql_comments(text))
    else:
        return True

    problems = []
    for st in statements:
        st = strip_sql_comments(st)
        amap = alias_map(st)
        if not amap:
            continue
        problems += check_statement(st, amap)

    if path.suffix == ".kt":
        problems += check_declared_constants(text, path.name)

    problems = sorted(set(problems))
    if problems:
        print(f"\n=== {path.name} ===")
        for p in problems:
            print(f"  [FAIL] {p}")
        return False
    n = sum(1 for st in statements if alias_map(st))
    print(f"  {path.name:32s} {n} SQL statement(s) checked - OK")
    return True



def selftest():
    """Prove the guard catches a guessed column in an INSERT/UPDATE, not only SELECT.

    Real motivation: the app's write path is INSERT into dbo.subsailtemp_pish and
    UPDATE dbo.sailfact_pish; before this, alias_map only saw FROM/JOIN, so those
    two statements were silently skipped and their column names unchecked.
    """
    good_insert = "INSERT INTO dbo.subsailtemp_pish (mod, shfacfo, rdf__, rdf, linesum, active) VALUES (1, ?, ?, ?, ?, ?)"
    bad_insert = "INSERT INTO dbo.subsailtemp_pish (mod, shfacfo, rdf__, rdf, linesum, aktiv) VALUES (1, ?, ?, ?, ?, ?)"
    bad_update = "UPDATE dbo.sailfact_pish SET active = 'f', ismodofy = 't' WHERE shfacfo = ?"
    good_update = "UPDATE dbo.sailfact_pish SET active = 'f', ismodify = 't' WHERE shfacfo = ? AND active = 't'"
    cases = [
        ("INSERT with real columns", good_insert, True),
        ("INSERT with a guessed column", bad_insert, False),
        ("UPDATE with a guessed column", bad_update, False),
        ("UPDATE with real columns", good_update, True),
    ]
    failed = 0
    for name, sql, expect_ok in cases:
        amap = alias_map(sql)
        problems = check_statement(sql, amap)
        ok = not problems
        mark = "ok" if ok == expect_ok else "WRONG"
        print(f"  [{mark}] {name}: {'clean' if ok else problems[0]}")
        if ok != expect_ok:
            failed += 1
    print("SELFTEST:", "PASSED" if failed == 0 else f"{failed} case(s) wrong")
    return failed == 0


SCHEMA = load_schema()
VALUES = load_values()

if __name__ == "__main__":
    args = sys.argv[1:]
    if args and args[0] == "--selftest":
        sys.exit(0 if selftest() else 1)
    files = [pathlib.Path(a) for a in args] if args else \
        sorted(list(ROOT.glob("*.kt")) + list(ROOT.glob("*.sql")))
    print(f"schema: {len(SCHEMA)} tables, {sum(len(v) for v in SCHEMA.values())} columns, "
          f"{len(VALUES)} verified values (from {TSV.name} + {VALUES_TSV.name})")
    ok = all(check(f) for f in files)
    print("\nRESULT:", "NO UNKNOWN IDENTIFIERS" if ok else "PROBLEMS FOUND")
    sys.exit(0 if ok else 1)
