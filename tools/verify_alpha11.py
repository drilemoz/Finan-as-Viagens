#!/usr/bin/env python3
"""SQLite regressions for alpha.11 paging and transactional preferences.
Runs without Android; Android SQLiteOpenHelper instrumentation is separate.
"""
from pathlib import Path
import sqlite3
import re

root = Path(__file__).resolve().parents[1]
src = (root / 'app/src/main/java/br/com/adriedson/financasdaviagem/data/FinanceRepository.kt').read_text()
helper = (root / 'app/src/main/java/br/com/adriedson/financasdaviagem/data/LedgerDb.kt').read_text()
assert 'SQLiteOpenHelper(appContext, name, null, 6)' in helper
assert 'if (oldVersion < 6) createSettings(db)' in helper
assert 'saveSettings(backup.settings)\n            writable.setTransactionSuccessful()' in src
assert 'FROM entries WHERE ${where.joinToString(" AND ")}' in src
assert 'ORDER BY created_at DESC,id DESC LIMIT ?' in src
assert 'fun historyPage(' in src
assert 'getSharedPreferences' not in src

conn = sqlite3.connect(':memory:')
conn.execute('CREATE TABLE entries(id INTEGER PRIMARY KEY, kind TEXT, created_at INTEGER, note TEXT)')
# A dense dataset exercises ties, crossed filters, and an incomplete final page.
items = [(i, 'income' if i%3==0 else 'expense', 10_000+i//13, f'item {i}') for i in range(1, 503)]
conn.executemany('INSERT INTO entries VALUES(?,?,?,?)', items)
def page(kind=None, start=0, cursor=None, size=60):
    parts=['created_at>=?']; args=[start]
    if kind:
        parts.append('kind=?'); args.append(kind)
    if cursor:
        parts.append('(created_at<? OR (created_at=? AND id<?))')
        args += [cursor[0],cursor[0],cursor[1]]
    args.append(size+1)
    rows=conn.execute('SELECT id,kind,created_at,note FROM entries WHERE '+ ' AND '.join(parts)+' ORDER BY created_at DESC,id DESC LIMIT ?',args).fetchall()
    return rows[:size],len(rows)>size
for kind,start in [(None,0),('income',0),('expense',10021),('income',10032),('expense',999_999)]:
    expected=conn.execute('SELECT id,kind,created_at,note FROM entries WHERE created_at>=?'+(' AND kind=?' if kind else '')+' ORDER BY created_at DESC,id DESC',([start,kind] if kind else [start])).fetchall()
    got=[]; cursor=None
    for _ in range(30):
        rows,more=page(kind,start,cursor)
        got.extend(rows)
        if not more: break
        cursor=rows[-1][2],rows[-1][0]
    assert expected==got,(kind,start,len(expected),len(got))

# The old database can be upgraded by adding app_settings only.
conn.executescript('''
CREATE TABLE accounts(id INTEGER PRIMARY KEY, name TEXT);
INSERT INTO accounts VALUES(1, 'Carteira pré-existente');
CREATE TABLE app_settings(
    id INTEGER PRIMARY KEY CHECK(id=1),
    initial_cents INTEGER NOT NULL,reserve_cents INTEGER NOT NULL,
    daily_budget_cents INTEGER NOT NULL,theme_mode TEXT NOT NULL,
    cans_per_kg INTEGER NOT NULL,cans_price_cents_kg INTEGER NOT NULL,
    trip_goal_cents INTEGER NOT NULL,planned_trip_days INTEGER NOT NULL,
    budget_alert_percent INTEGER NOT NULL);
INSERT INTO app_settings VALUES(1,10000,2000,4000,'dark',70,600,200000,100,80);
''')
assert conn.execute('SELECT name FROM accounts WHERE id=1').fetchone()[0]=='Carteira pré-existente'
# Simulates a crash/failure after modifying settings and before the financial rows commit.
old=conn.execute('SELECT * FROM app_settings').fetchone()
try:
    with conn:
        conn.execute("UPDATE app_settings SET theme_mode='light', daily_budget_cents=2500")
        conn.execute("INSERT INTO entries(id,kind,created_at,note) VALUES(1,'income',1,'duplicate')")
except sqlite3.IntegrityError:
    pass
assert conn.execute('SELECT * FROM app_settings').fetchone()==old, 'Settings changed despite rollback'
print('OK: keyset pages (ties and filters), schema v6, rollback of preferences and financial rows.')
