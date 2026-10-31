#!/usr/bin/env python3
"""Verificações estruturais da versão 1.0.0. Não substitui Gradle/Play Console."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
get = lambda f: (root / f).read_text(encoding='utf-8')
gradle = get('app/build.gradle.kts')
main = get('app/src/main/java/br/com/adriedson/financasdaviagem/MainActivity.kt')
manifest = get('app/src/main/AndroidManifest.xml')
release = get('.github/workflows/android-play.yml')
debug = get('.github/workflows/android.yml')
privacy = get('app/src/main/java/br/com/adriedson/financasdaviagem/ui/PrivacyPolicyScreen.kt')
settings = get('app/src/main/java/br/com/adriedson/financasdaviagem/ui/SettingsAndAboutScreen.kt')
ledger = get('app/src/main/java/br/com/adriedson/financasdaviagem/data/LedgerDb.kt')
repo = get('app/src/main/java/br/com/adriedson/financasdaviagem/data/FinanceRepository.kt')
root_xml = ET.fromstring(manifest)
checks = {
    'SDK Android 16': 'compileSdk = 36' in gradle and 'targetSdk = 36' in gradle and 'minSdk = 26' in gradle,
    'versionamento estável': 'versionCode = 13' in gradle and 'versionName = "1.0.0"' in gradle,
    'pacote original': 'br.com.adriedson.financasdaviagem' in gradle and 'android:name=".MainActivity"' in manifest,
    'nenhuma permissão extra': not root_xml.findall('uses-permission') and 'usesCleartextTraffic="false"' in manifest,
    'backup Android desabilitado': 'allowBackup="false"' in manifest,
    'EdgeToEdge': 'enableEdgeToEdge()' in main and 'windowSoftInputMode="adjustResize"' in manifest,
    'sem opt-out EdgeToEdge': 'windowOptOutEdgeToEdgeEnforcement' not in manifest,
    'assinatura apenas de ambiente': 'SIGNING_KEYSTORE_FILE' in gradle and 'storeType = "PKCS12"' in gradle and 'playUpload' in gradle,
    'AAB Release assinado': ':app:bundleRelease' in release and 'jarsigner -verify' in release and 'apksigner' in release,
    'secrets GitHub': all('secrets.' + k in release for k in ('KEYSTORE_BASE64','KEYSTORE_PASSWORD','KEY_ALIAS','KEY_PASSWORD')),
    'sem publicar chave': 'release-output/*' in release and 'cp "$SIGNING_KEYSTORE_FILE"' not in release,
    'debug atualizado': "platforms;android-36" in debug and 'v1.0.0-APK-Debug' in debug,
    'política acessível': '"privacy" -> PrivacyPolicyScreen' in main and 'onPrivacy = { screen = "privacy" }' in main and 'Política de privacidade' in settings,
    'política pública para hospedagem': '<html lang="pt-BR">' in get('docs/privacy-policy.html'),
    'instruções de publicação': 'android-play.yml' in get('docs/GOOGLE_PLAY.md'),
    'sem migração SQL': 'SQLiteOpenHelper(appContext, name, null, 6)' in ledger and '"schemaVersion", 6' in repo,
    'sobre sincronizado': 'Versão 1.0.0 · versionCode 13' in settings,
    'sem keystore no código': not any(p.suffix.lower() in {'.p12','.jks','.keystore','.env'} for p in root.rglob('*') if p.is_file()),
}
for name, ok in checks.items():
    print(f"{'OK' if ok else 'ERRO'} - {name}")
assert all(checks.values()), 'Verificações Google Play falharam'
print(f'{len(checks)} verificações estruturais aprovadas')
