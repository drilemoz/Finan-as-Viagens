# Validação da alpha.2

Origem: logs enviados do GitHub Actions, workflow **Gerar APK**, execução 1, em 2026-10-27.

## Falha comprovada

- `Warning: Failed to find package 'tools'`
- `Error: The process .../sdkmanager failed with exit code 1`
- A etapa com `android-actions/setup-android@v3` abortou antes de instalar a plataforma e antes dos testes/Gradle.
- Nenhum APK foi criado; upload-artifact avisou que não encontrou arquivos.

## Correção

- `android-actions/setup-android@v4`, `packages: 'platform-tools'`, sem pacote obsoleto.
- Instalação explícita posterior de `platforms;android-35` e `build-tools;35.0.0`.
- GitHub Actions continua usando Gradle 8.13, JDK 17 e AGP 8.11.1.

## Limites de validação

A CI da alpha.2 **não foi executada** nesta entrega; verificações de estrutura e matemática locais não substituem o build real Android nem teste no aparelho. Caso o build falhe mais adiante, o novo log permitirá diagnosticar a etapa seguinte.

## alpha.3
- Verificar compatibilidade do schema SQLite 1→2, back-ups schema 1/2, fórmula quantidade × preço e modo claro/escuro.
- Build Gradle e teste Android físico pendentes neste ambiente.
