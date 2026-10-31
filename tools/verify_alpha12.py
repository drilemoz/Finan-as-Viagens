#!/usr/bin/env python3
"""Guardrails do layout compacto introduzido na alpha.12."""
from pathlib import Path
import re

root = Path(__file__).resolve().parent.parent
screens = (root / 'app/src/main/java/br/com/adriedson/financasdaviagem/ui/Screens.kt').read_text()
components = (root / 'app/src/main/java/br/com/adriedson/financasdaviagem/ui/Components.kt').read_text()
about = (root / 'app/src/main/java/br/com/adriedson/financasdaviagem/ui/SettingsAndAboutScreen.kt').read_text()
gradle = (root / 'app/build.gradle.kts').read_text()
workflow = (root / '.github/workflows/android.yml').read_text()
old = Path('/mnt/data/financas_alpha11_work')
checks = {
    'versão Android': 'versionCode = 13' in gradle and 'versionName = "1.0.0"' in gradle,
    'versão Sobre': 'Versão 1.0.0' in about and 'versionCode 13' in about,
    'artefato CI': 'Financas-da-Viagem-v1.0.0-APK-Debug-e-relatorios' in workflow,
    'receita / doação mantidas': all(x in screens for x in ['onClick = onAddIncome', 'onClick = onAddDonation', 'Text("Receita"', 'Text("Doação"']),
    'despesa continua destacada': 'onClick = onAddExpense' in screens and 'Text("Adicionar despesa"' in screens,
    'três atalhos conectados': all(x in screens for x in [
        'DashboardShortcut("Planejar", Icons.Outlined.Flag, onPlanning',
        'DashboardShortcut("Relatórios", Icons.Outlined.BarChart, onReports',
        'DashboardShortcut("Gerenciar", Icons.Outlined.EventNote, onBills']),
    'altura segura dos atalhos': 'modifier.heightIn(min = 72.dp)' in screens,
    'altura segura das duas entradas': screens.count('Modifier.weight(1f).heightIn(min = 48.dp)') == 2,
    'cartões e estado vazio opcionais': 'compact: Boolean = false' in components and screens.count('compact = true') == 5,
    'espaçamento reduzido': 'verticalArrangement = Arrangement.spacedBy(12.dp)' in screens,
    'sem novo esquema SQLite': 'LedgerDb.kt' in [str(p.name) for p in (root / 'app/src/main/java/br/com/adriedson/financasdaviagem/data').glob('*.kt')],
}
for name, good in checks.items():
    print(('OK' if good else 'ERRO') + ' - ' + name)
assert all(checks.values()), 'A alpha.12 falhou na validação estrutural'
print(f'{len(checks)} verificações aprovadas')
