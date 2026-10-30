#!/usr/bin/env python3
"""Regression smoke test without Android: SQL ledger balances vs integer reference model.
Run from the project root with: python3 tools/verify_refactor.py
This does not replace SQLiteOpenHelper Android instrumentation or a real APK build.
"""
from pathlib import Path
import random
import re
import sqlite3

root = Path(__file__).resolve().parents[1]
source = (root / 'app/src/main/java/br/com/adriedson/financasdaviagem/data/WalletBalanceQueries.kt').read_text()
match = re.search(r'private const val SQL = """(.*?)"""', source, re.S)
assert match, 'Consulta de saldos não encontrada'
query = match.group(1)
connection = sqlite3.connect(':memory:')
connection.executescript('''
CREATE TABLE accounts(id INTEGER PRIMARY KEY, opening_cents INTEGER NOT NULL);
CREATE TABLE entries(kind TEXT NOT NULL, brl_cents INTEGER NOT NULL, account_id INTEGER NOT NULL);
CREATE TABLE wallet_transfers(from_id INTEGER NOT NULL, to_id INTEGER NOT NULL, cents INTEGER NOT NULL);
''')
connection.executemany('INSERT INTO accounts VALUES (?,?)', [(1, 20000), (2, 5000), (3, 0)])
for seed in range(100):
    randomizer = random.Random(seed)
    connection.execute('DELETE FROM entries')
    connection.execute('DELETE FROM wallet_transfers')
    expected = {1: 20000, 2: 5000, 3: 0}
    for _ in range(160):
        account = randomizer.choice(tuple(expected))
        amount = randomizer.randint(1, 50000)
        if randomizer.randrange(2):
            kind = randomizer.choice(('income', 'expense'))
            connection.execute('INSERT INTO entries VALUES (?,?,?)', (kind, amount, account))
            expected[account] += amount if kind == 'income' else -amount
        else:
            destination = randomizer.choice(tuple(x for x in expected if x != account))
            connection.execute('INSERT INTO wallet_transfers VALUES (?,?,?)', (account, destination, amount))
            expected[account] -= amount
            expected[destination] += amount
    for account, balance in expected.items():
        got = connection.execute(query, (account,)).fetchone()
        assert got is not None and got[0] == balance, f'case={seed} account={account}, got={got}, expected={balance}'
assert connection.execute(query, (999,)).fetchone() is None
print('OK: 100 cenários × 160 lançamentos/transferências e conta inexistente.')
