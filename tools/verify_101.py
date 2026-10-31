#!/usr/bin/env python3
"""Verificações locais da atualização 1.0.1; não substituem compilação Android."""
from pathlib import Path
import re
r = Path(__file__).resolve().parents[1]
get = lambda p: (r/p).read_text(encoding='utf-8')
screens = get('app/src/main/java/br/com/adriedson/financasdaviagem/ui/Screens.kt')
summary = get('app/src/main/java/br/com/adriedson/financasdaviagem/core/ScreenSummaries.kt')
workflow = get('.github/workflows/android-play.yml')
app = get('app/build.gradle.kts')
checks = {
    'ganhos diários no painel': 'ValueCard("Ganhos hoje", FinanceMath.format(todayIncome)' in screens,
    'autonomia removida só do painel': 'Dias de autonomia' not in screens and 'autonomyDays' in get('app/src/main/java/br/com/adriedson/financasdaviagem/core/TripPlanningMath.kt'),
    'categoria só despesas': 'SectionTitle("Gastos por categoria")' in screens and 'entry.kind == "expense"' in summary,
    'resultado separado do saldo': 'Resultado de hoje' in screens and 'totals.todayNetCents' in screens,
    'receitas apenas do dia': 'entry.createdAt in dayStartMillis..nowMillis' in summary and 'todayIncome = Math.addExact' in summary,
    'sem dinheiro futuro implicitamente': 'entry.createdAt in dayStartMillis..nowMillis' in summary,
    'dois arquivos Play separados': workflow.count('uses: actions/upload-artifact@v7') == 2,
    'sem ZIP externo nos artifacts': workflow.count('archive: false') >= 2 and 'release-output/*' not in workflow,
    'AAB e APK mantidos': 'v1.0.1-Play.aab' in workflow and 'v1.0.1-Release.apk' in workflow,
    'versionamento': 'versionCode = 14' in app and 'versionName = "1.0.1"' in app,
    'mesmo SQLite': 'SQLiteOpenHelper(appContext, name, null, 6)' in get('app/src/main/java/br/com/adriedson/financasdaviagem/data/LedgerDb.kt'),
    'mesmo backup': '"schemaVersion", 6' in get('app/src/main/java/br/com/adriedson/financasdaviagem/data/FinanceRepository.kt'),
}
for name,ok in checks.items(): print(('OK' if ok else 'ERRO')+' - '+name)
assert all(checks.values())
print(f'{len(checks)} verificações aprovadas')
