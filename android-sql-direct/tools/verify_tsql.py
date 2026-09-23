#!/usr/bin/env python3
"""
Static verification for the Vizitor SQL scripts (no SQL Server needed).

Why this exists: these scripts are executed by hand on a production ERP server,
where a single syntax mistake costs a round-trip with the operator. This tool
catches the whole class of mistakes that already burned us once:

  1. every batch (split on GO) is parsed with a real T-SQL parser (sqlglot)
  2. every string that the script builds dynamically (EXEC / sp_executesql) is
     reconstructed and parsed as well
  3. executable code is checked to be pure ASCII (Persian text is allowed in
     comments only, because SSMS 2014 encoding of non-ASCII inside code is risky)
  4. known-bad catalog columns are rejected (sys.index_columns has no
     is_primary_key; sys.parameters has no PARAMETER_NAME)
  5. every variable is declared in the same GO-batch that uses it: a DECLARE
     above a GO is invisible below it, which is exactly how part 7 v1 died
     with "Must declare the scalar variable @testUser" (Msg 137)

Run it with:  python3 tools/verify_tsql.py            (checks all of sql/*.sql)
              python3 tools/verify_tsql.py --selftest (checks the checker itself)

NOTE: this is a PYTHON tool. It must never be opened or run inside SSMS.
Requires: pip install sqlglot
"""
import re
import sys
import pathlib

try:
    import sqlglot
except ImportError:  # pragma: no cover
    sys.exit("sqlglot is required:  pip install sqlglot")

MARK = "\x00"  # sentinel used while tokenizing T-SQL string literals


def strip_comments(text: str) -> str:
    """Remove /*...*/ and --... comments while leaving string literals alone.

    A plain regex is wrong here: '---' inside a literal (used as a separator in
    printed output) would be eaten as a comment and unbalance the quotes, which
    is how part 7 v2 first failed this tool's parse. Line breaks are preserved.
    """
    out, i, n = [], 0, len(text)
    while i < n:
        c = text[i]
        if c == "'" or (c in "Nn" and text[i + 1:i + 2] == "'"):
            j = i + 2 if c != "'" else i + 1
            while j < n:
                if text[j] == "'":
                    if text[j + 1:j + 2] == "'":
                        j += 2
                        continue
                    j += 1
                    break
                j += 1
            out.append(text[i:j])
            i = j
        elif text.startswith("/*", i):
            j = text.find("*/", i + 2)
            j = n if j == -1 else j + 2
            out.append("".join(ch if ch == "\n" else " " for ch in text[i:j]))
            i = j
        elif text.startswith("--", i):
            j = text.find("\n", i)
            j = n if j == -1 else j
            out.append(" " * (j - i))
            i = j
        else:
            out.append(c)
            i += 1
    return "".join(out)


def batches(text: str):
    text = strip_comments(text)
    return [b for b in re.split(r"(?im)^\s*GO\s*$", text) if b.strip()]


def parse(sql: str) -> str:
    """Return '' when the statement parses, otherwise the error message."""
    try:
        sqlglot.parse(sql, read="tsql")
        return ""
    except Exception as exc:  # noqa: BLE001
        return str(exc).splitlines()[0]


SAMPLES = {"@sch": "dbo", "@tbl": "sys_users", "@col": "Password",
           "@dbName": "Meelano", "@dbname": "Meelano"}


def split_chain(expr: str):
    """Split a T-SQL concatenation chain into literal and non-literal operands."""
    parts, buf, i = [], "", 0
    while i < len(expr):
        c = expr[i]
        if (c in "Nn" and expr[i + 1:i + 2] == "'") or c == "'":
            start = i + 2 if c != "'" else i + 1
            j = start
            while j < len(expr):
                if expr[j] == "'":
                    if expr[j + 1:j + 2] == "'":
                        j += 2
                        continue
                    break
                j += 1
            if j >= len(expr):
                raise ValueError("unterminated string literal in chain")
            buf += MARK + expr[start:j].replace("''", "'") + MARK
            i = j + 1
        elif c == "+":
            if buf.strip():
                parts.append(buf.strip())
            buf = ""
            i += 1
        else:
            buf += c
            i += 1
    if buf.strip():
        parts.append(buf.strip())
    return parts


def chain_of(text: str, var: str, before: int) -> str:
    """Text of the assignment that produced @var just before the EXEC at
    position `before`. Handles both `SET @v = ...` / `SET @v = @v + ...` and
    `DECLARE @v <type> = ...`, and ignores semicolons inside string literals."""
    head = text[:before]
    patterns = [
        r"SET\s+@" + var + r"\s*=\s*@" + var + r"\s*\+",   # accumulating loop
        r"SET\s+@" + var + r"\s*=",
        r"DECLARE\s+@" + var + r"\s+\w+\s*(?:\([^)]*\))?\s*=",
    ]
    best = None                                   # (end position of the match)
    for pat in patterns:
        for m in re.finditer(pat, head, flags=re.I):
            if best is None or m.end() > best:
                best = m.end()
    if best is None:
        raise ValueError(f"no DECLARE/SET for @{var}")

    i, paren = best, 0
    while i < len(text):
        c = text[i]
        if (c in "Nn" and text[i + 1:i + 2] == "'") or c == "'":
            j = i + 2 if c != "'" else i + 1
            while j < len(text):
                if text[j] == "'":
                    if text[j + 1:j + 2] == "'":
                        j += 2
                        continue
                    break
                j += 1
            i = j + 1
            continue
        if c == "(":
            paren += 1
        elif c == ")":
            paren -= 1
        elif c == ";" and paren == 0:
            break
        i += 1
    return text[best:i]


def eval_operand(token: str) -> str:
    t = token.strip()
    if not t:
        return ""
    if t in SAMPLES:
        return SAMPLES[t]
    if t.startswith("@"):
        return "\x00"                                 # runtime variable: stand-in
    up = t.upper()
    if up.startswith("QUOTENAME(") and t.endswith(")"):
        return "[" + eval_operand(t[t.index("(") + 1:t.rindex(")")]).strip("[]") + "]"
    if up.startswith("REPLACE(") and t.endswith(")"):
        inner = t[t.index("(") + 1:t.rindex(")")]
        args, depth, buf = [], 0, ""
        for ch in inner:                              # split on top-level commas
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
            if ch == "," and depth == 0:
                args.append(buf)
                buf = ""
            else:
                buf += ch
        args.append(buf)
        if len(args) == 3:
            return eval_operand(args[0]).replace(eval_operand(args[1]), eval_operand(args[2]))
    if "+" in t:
        return "".join(eval_operand(p) for p in split_chain(t))
    # anything else (CAST/ISNULL/... over a variable): keep a stand-in so the
    # surrounding SQL stays syntactically complete for the candidate checks
    return "\x00" if "@" in t else ""


def simulate_dynamic_sql(text: str):
    """Reconstruct every statement the script concatenates at runtime.

    Done per GO-batch on purpose: variables do not survive a GO separator, so
    the assignment that matters is the one inside the same batch as the EXEC."""
    out = []
    for batch in re.split(r"(?im)^\s*GO\s*$", text):
        for m in re.finditer(r"(?:EXEC|EXECUTE)\s*\(\s*@(\w+)\s*\)", batch, flags=re.I):
            var = m.group(1)
            try:
                chain = chain_of(batch, var, m.start())
                generated = "".join(
                    p[1:-1] if p.startswith(MARK) and p.endswith(MARK) else eval_operand(p)
                    for p in split_chain(chain))
            except Exception as exc:                  # noqa: BLE001
                out.append((var, "", f"{type(exc).__name__}: {exc}"))
                continue
            out.append((var, generated, ""))
    return out


RESERVED = {
    "add", "all", "alter", "and", "any", "as", "asc", "authorization", "backup",
    "begin", "between", "break", "browse", "bulk", "by", "cascade", "case",
    "check", "checkpoint", "close", "clustered", "coalesce", "collate", "column",
    "commit", "compute", "constraint", "contains", "containstable", "continue",
    "convert", "create", "cross", "current", "current_date", "current_time",
    "current_timestamp", "current_user", "cursor", "database", "dbcc",
    "deallocate", "declare", "default", "delete", "deny", "desc", "disk",
    "distinct", "distributed", "double", "drop", "dump", "else", "end", "errlvl",
    "escape", "except", "exec", "execute", "exists", "exit", "external", "fetch",
    "file", "fillfactor", "for", "foreign", "freetext", "freetexttable", "from",
    "full", "function", "goto", "grant", "group", "having", "holdlock",
    "identity", "identity_insert", "identitycol", "if", "in", "index", "inner",
    "insert", "intersect", "into", "is", "join", "key", "kill", "left", "like",
    "lineno", "load", "merge", "national", "nocheck", "nonclustered", "not",
    "null", "nullif", "of", "off", "offsets", "on", "open", "opendatasource",
    "openquery", "openrowset", "openxml", "option", "or", "order", "outer",
    "over", "percent", "pivot", "plan", "precision", "primary", "print", "proc",
    "procedure", "public", "raiserror", "read", "readtext", "reconfigure",
    "references", "replication", "restore", "restrict", "return", "revert",
    "revoke", "right", "rollback", "rowcount", "rowguidcol", "rule", "save",
    "schema", "securityaudit", "select", "session_user", "set", "setuser",
    "shutdown", "some", "statistics", "system_user", "table", "tablesample",
    "textsize", "then", "to", "top", "tran", "transaction", "trigger",
    "truncate", "try_convert", "tsequal", "union", "unique", "unpivot",
    "update", "updatetext", "use", "user", "values", "varying", "view",
    "waitfor", "when", "where", "while", "with", "writetext",
}


def check_reserved_identifiers(text: str):
    """Object names that are reserved keywords must be bracketed.

    Real case: 'FROM EMS.user' raised Msg 156 'Incorrect syntax near the
    keyword user' on the live server. Correct: 'FROM [EMS].[user]'."""
    problems = []
    code = strip_comments(text)
    # ... FROM/JOIN schema.object  (object may be bracketed)
    for m in re.finditer(r"\b(?:FROM|JOIN|INTO|UPDATE|EXEC(?:UTE)?)\s+((?:\[?\w+\]?\.)*)(\[?\w+\]?)", code, flags=re.I):
        parts = [p for p in m.group(1).split(".") if p]
        last = m.group(2)
        for part in parts + [last]:
            bare = part.strip("[]")
            if part.startswith("[") and part.endswith("]"):
                continue
            if bare.lower() in RESERVED:
                problems.append(
                    f"'{bare}' is a reserved T-SQL keyword and must be bracketed "
                    f"(write [{bare}]) in: {m.group(0).strip()[:60]}")
    return sorted(set(problems))


def check_variable_scope(text: str):
    """Variables must be declared in the same GO-batch that uses them.

    Real case (part 7 v1): DECLARE @testUser ... GO ... WHERE user_name =
    @testUser -> the batch below the GO fails to compile with
    "Msg 137 Must declare the scalar variable" and produces no rows at all.

    Temp tables (#x) do survive a GO, so they are not checked here."""
    problems = []
    for n, batch in enumerate(batches(text), 1):
        code = re.sub(r"'(?:[^']|'')*'", "''", batch)   # drop string literals
        declared = set()
        for m in re.finditer(r"\bDECLARE\s+@", code, flags=re.I):
            end = code.find(";", m.end())               # one declarator list
            stmt = code[m.start():end if end != -1 else len(code)]
            declared.update(v.lower() for v in re.findall(r"(?<!@)@(\w+)", stmt))
        used = {v.lower() for v in re.findall(r"(?<!@)@(\w+)", code)}
        for var in sorted(used - declared):
            problems.append(
                f"batch {n} uses @{var} but does not declare it in that batch "
                f"(a DECLARE above a GO is invisible below it -> Msg 137, "
                f"and the whole batch produces no output)")
    return problems


def check_isnull_truncation(text: str):
    """ISNULL returns the type of its FIRST argument - a longer replacement is cut.

    Real case (part 7 v2, live run 2026-09-18): the login proof line printed
    IsLocked=nu for a NULL value. The expression was
    ISNULL(CAST(u.IsLocked AS NVARCHAR(2)), N'null'), so the replacement literal
    'null' was silently truncated to 'nu' and looked like a weird schema value.
    COALESCE does not have this problem (it takes the widest argument)."""
    problems = []
    code = strip_comments(text)
    pat = re.compile(
        r"ISNULL\s*\(\s*CAST\s*\(.*?\s+AS\s+(n?varchar|n?char)\s*\(\s*(\d+)\s*\)\s*\)"
        r"\s*,\s*N?'((?:[^']|'')*)'",
        flags=re.I)
    for m in pat.finditer(code):
        type_name, width, lit = m.group(1).lower(), int(m.group(2)), m.group(3)
        if len(lit) > width:
            problems.append(
                f"ISNULL(CAST(... AS {type_name}({width})), N'{lit}') prints only "
                f"N'{lit[:width]}' - ISNULL keeps the type of its first argument, "
                f"so widen the CAST (or use CASE WHEN ... IS NULL)")
    return sorted(set(problems))


def split_statements(batch: str):
    """Split one batch at top-level semicolons (depth 0, BEGIN/END balanced).

    Used when a T-SQL parser cannot swallow a whole batch even though every
    statement in it is valid: some parsers fall back to a raw 'Command' node
    for TRY/CATCH blocks and then lose track of the rest of the batch."""
    out, start, i = [], 0, 0
    depth, in_lit = 0, False
    while i < len(batch):
        ch = batch[i]
        if in_lit:
            if ch == "'":
                if batch[i + 1:i + 2] == "'":
                    i += 2
                    continue
                in_lit = False
            i += 1
            continue
        if ch == "'":
            in_lit = True
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        elif ch == ";":
            head = batch[start:i + 1]
            if depth == 0 and (len(re.findall(r"\bBEGIN\b", head, re.I))
                               == len(re.findall(r"\bEND\b", head, re.I))):
                out.append(head)
                start = i + 1
        i += 1
    tail = batch[start:].strip()
    if tail:
        out.append(tail)
    return [x for x in out if x.strip()]


def begin_end_balanced(batch: str):
    """BEGIN/END and BEGIN TRY/BEGIN CATCH versus END counts."""
    code = re.sub(r"'[^']*(?:''[^']*)*'", "''", batch)      # drop string literals
    begins = len(re.findall(r"\bBEGIN\b", code, re.I))
    ends = len(re.findall(r"\bEND\b", code, re.I))
    cases = len(re.findall(r"\bCASE\b", code, re.I))
    trys = len(re.findall(r"\bBEGIN\s+TRY\b", code, re.I))
    catches = len(re.findall(r"\bBEGIN\s+CATCH\b", code, re.I))
    # every CASE expression also ends with END, so it pairs with one END too
    return (begins + cases) - ends, trys - catches


def check(path: pathlib.Path) -> bool:
    text = path.read_text(encoding="utf-8-sig")
    ok = True
    print(f"\n=== {path.name} ===")

    batch_list = batches(text)
    whole_fail, stmt_fail = [], []
    for i, b in enumerate(batch_list, 1):
        err = parse(b)
        if not err:
            continue
        # a parser may choke on a whole batch that is valid: verify statement by
        # statement before reporting a problem
        bad = [(st, parse(st)) for st in split_statements(b)]
        bad = [(st, e) for st, e in bad if e]
        if bad:
            stmt_fail.append((i, bad[0][1], bad[0][0]))
            whole_fail.append((i, err))
        else:
            whole_fail.append((i, None))

    if stmt_fail:
        ok = False
        for i, err, st in stmt_fail:
            print(f"  [FAIL] batch {i} statement: {err[:140]}")
            print("         " + st.strip().replace("\n", " ")[:200])
    elif whole_fail:
        print(f"  batches parsed .............. {len(batch_list)}/{len(batch_list)} OK "
              f"(batch {whole_fail[0][0]} verified statement-by-statement: "
              "the parser cannot read TRY/CATCH batches as a whole)")
    else:
        print(f"  batches parsed .............. {len(batch_list)}/{len(batch_list)} OK")

    for i, b in enumerate(batch_list, 1):
        be, tc = begin_end_balanced(b)
        if be != 0 or tc != 0:
            ok = False
            print(f"  [FAIL] batch {i}: BEGIN/END differ by {be}, TRY/CATCH by {tc}")

    for var, gen, err0 in simulate_dynamic_sql(text):
        if err0:
            ok = False
            print(f"  [FAIL] @{var}: could not reconstruct ({err0})")
            continue
        if not gen.strip():
            print(f"  [skip] @{var}: runtime variables only, no literal text")
            continue

        # the chain may mix literals with variables (table names, column lists).
        # Try a few harmless stand-ins; if none parses, the literal parts are
        # broken. Report which stand-in was needed.
        candidates = [("", gen)]
        for sub in ("[Meelano]", "N'x'", "1"):
            candidates.append((sub, gen.replace("\x00", sub)))
        err = None
        for used, cand in candidates:
            err = parse(cand)
            if not err:
                note = "" if used == "" else f" [variable stand-in {used}]"
                print(f"  dynamic SQL @{var} ........... OK "
                      f"({len(cand)} chars, quotes balanced: "
                      f"{cand.count(chr(39)) % 2 == 0}){note}")
                break
        else:
            ok = False
            print(f"  [FAIL] dynamic SQL of @{var}: {err[:160]}")
            print("         generated: " + gen[:400].replace("\n", " "))

    raw = path.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        ok = False
        print("  [FAIL] file starts with a UTF-8 BOM: SSMS passes it into the first "
              "batch -> \"Msg 102 Incorrect syntax near '\". Save as plain UTF-8.")
    else:
        print("  no BOM ..................... OK")

    bad = sorted({c for c in text if ord(c) > 127})
    if bad:
        ok = False
        print(f"  [FAIL] non-ASCII characters present: {[hex(ord(c)) for c in bad][:10]}"
              "  (SQL files must be pure ASCII, comments included)")
    else:
        print("  file is pure ASCII ......... OK (comments included)")

    # independent check: parentheses must balance inside every batch, ignoring
    # string literals and comments (caught a real missing ')' in v5 section 04
    # that a T-SQL parser only reports as a confusing "Expecting )" elsewhere)
    for n, batch in enumerate(batch_list, 1):
        depth, in_lit, k, bad = 0, False, 0, False
        while k < len(batch):
            ch = batch[k]
            if in_lit:
                if ch == "'":
                    if batch[k + 1:k + 2] == "'":
                        k += 2
                        continue
                    in_lit = False
            elif ch == "'":
                in_lit = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth < 0:
                    bad = True
                    break
            k += 1
        if bad or depth != 0:
            ok = False
            print(f"  [FAIL] batch {n}: parentheses do not balance "
                  f"(final depth {depth}) - a missing or extra '(' or ')'")
    code = strip_comments(text)   # comments removed: used by the checks below

    banned = {
        "ic.is_primary_key": "sys.index_columns has no is_primary_key - join sys.indexes",
        "PARAMETER_NAME": "sys.parameters column is named 'name'",
        "sp_executesql STUFF": "EXEC argument list parse error (Msg 102) - use EXEC (@sql)",
    }
    for token, why in banned.items():
        if token in code:
            ok = False
            print(f"  [FAIL] banned construct '{token}': {why}")
    if not any(t in code for t in banned):
        print("  known-bad constructs ........ none")

    reserved = check_reserved_identifiers(text)
    if reserved:
        ok = False
        for pr in reserved:
            print(f"  [FAIL] {pr}")
    else:
        print("  reserved keywords ............ OK (object names are bracketed)")

    leftovers = re.findall(r"(?:FROM|JOIN)\s+wanted\b", code, flags=re.I)
    if len(leftovers) > 1:
        ok = False
        print(f"  [FAIL] CTE 'wanted' used by {len(leftovers)} statements "
              f"(a CTE lives for one statement only) - use a table variable")
    else:
        print("  CTE scope ................... OK")

    scope = check_variable_scope(text)
    if scope:
        ok = False
        for pr in scope:
            print(f"  [FAIL] {pr}")
    else:
        print("  variable scope per batch .... OK (every @var is declared in its batch)")

    trunc = check_isnull_truncation(text)
    if trunc:
        ok = False
        for pr in trunc:
            print(f"  [FAIL] {pr}")
    else:
        print("  ISNULL return type .......... OK (no replacement literal is cut)")

    return ok


def selftest():
    """Prove the checker fires on the real mistakes it exists for."""
    buggy_scope = ("DECLARE @testUser NVARCHAR(80) = N'Admin';\nGO\n"
                   "SELECT user_name FROM dbo.sys_users WHERE user_name = @testUser;\n")
    good_scope = ("DECLARE @testUser NVARCHAR(80) = N'Admin';\n"
                  "SELECT user_name FROM dbo.sys_users WHERE user_name = @testUser;\n")
    cases = [
        ("variable used below a GO", check_variable_scope(buggy_scope), True),
        ("variable declared in its batch", check_variable_scope(good_scope), False),
        ("reserved object name unbracketed",
         check_reserved_identifiers("SELECT * FROM EMS.user;"), True),
        ("reserved object name bracketed",
         check_reserved_identifiers("SELECT * FROM [EMS].[user];"), False),
        ("ISNULL literal longer than the CAST width",
         check_isnull_truncation(
             "SELECT ISNULL(CAST(u.IsLocked AS NVARCHAR(2)), N'null') FROM dbo.sys_users u;"),
         True),
        ("ISNULL literal that fits the CAST width",
         check_isnull_truncation(
             "SELECT ISNULL(CAST(u.IsLocked AS NVARCHAR(10)), N'null') FROM dbo.sys_users u;"),
         False),
    ]
    failed = 0
    for name, found, should_find in cases:
        hit = bool(found)
        mark = "OK  " if hit == should_find else "FAIL"
        if hit != should_find:
            failed += 1
        print(f"  [{mark}] selftest: {name} -> {'caught' if hit else 'clean'}"
              f"{'' if hit == should_find else ' (wrong expectation)'}")
    print("  selftest result:", "all expectations met" if not failed else f"{failed} wrong")
    return failed == 0


def main(argv):
    if "--selftest" in argv:
        return 0 if selftest() else 1
    paths = [pathlib.Path(p) for p in argv[1:]] or sorted(
        pathlib.Path(__file__).resolve().parent.parent.glob("sql/*.sql"))
    all_ok = all(check(p) for p in paths)
    print("\nRESULT:", "ALL CHECKS PASSED" if all_ok else "PROBLEMS FOUND")
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
