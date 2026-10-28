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

## alpha.4 (2026-10-27)

- Sintaxe Kotlin: 11 arquivos inspecionados por PSI, 0 erros sintáticos.
- Core `FinanceMath.kt` + `RecyclingMath.kt`: compilação isolada com `kotlinc` aprovada.
- 14 verificações Kotlin executadas em runner local, cobrindo 70 latinhas/kg, R$6/kg, pesagem, arredondamento, entradas inválidas e compatibilidade monetária.
- Camada de dados: compilação Kotlin com stubs de Android/JSON concluída; **stubs não validam APIs Android reais**.
- SQLite via `sqlite3`: migrações simuladas v1→3 e v2→3, preservação de lançamentos, inserção de coleta/venda, materiais restritos por `CHECK`.
- Suite JUnit `RecyclingMathTest.kt` incluída no projeto para execução pela CI; os testes do Gradle ainda não foram executados aqui.
- Ainda sem build `:app:assembleDebug`, instalação ou teste em telefone Android. A integridade e a compatibilidade real devem ser verificadas no workflow GitHub.
- Backup schema 3 e restauração devem ser exercitados em aparelho antes de depender deles como único backup. Não desinstale sem backup válido.


## alpha.5 (2026-10-27)

- Verificar migração SQLite 1/2/3→4 em CI e no aparelho, com saldos iniciais, reserva e receita de reciclagem vinculada.
- Verificar transferência de dinheiro físico para Banco/Pix e reserva sem aumento de saldo total.
- Verificar backup JSON schema 4 (exportação/restore) e importação dos formatos 1, 2 e 3.
- Conferir edição de receitas/despesas e escolha de conta de recebimento de vendas.
- A compilação Gradle/Android e o teste de interface em aparelho ainda não foram executados neste ambiente.

## alpha.6 (2026-10-27)

- Verificar migração SQLite 4→5 e cadeia 1/2/3→5, mantendo estoque, saldos, carteiras e lançamentos.
- Validar `Usar todas` (80 latinhas → 1143 g estimados → R$ 6,86 na cotação de R$ 6/kg), evitando venda maior que estoque.
- Verificar doações na conta física, banco e reserva, sem duplicação do saldo.
- Verificar contas a pagar pendentes, vencidas e pagas; pagamento atômico vinculado e proteção do histórico; backups v1–v5; exportação CSV.
- Build Gradle/Android e teste de interface em aparelho ainda pendentes.

## alpha.7 (2026-10-27)

- Compilar e testar `MoneyInput` e `ReportMath` (camada pura), incluindo formatação de milhares, centavos, moedas de unidade inteira, agrupamento por categoria e seleção de período.
- Testar em Android: digitação, seleção e apagar valores em campos monetários (inclusive ao editar valor existente), colar texto, separadores, valores nulos, teclado numérico.
- Confirmar que a cotação manual e o peso medido em kg não são submetidos à máscara de moeda.
- Confirmar relatórios de Trabalho/Doação e que transferências, coletas não vendidas e saldos iniciais ficam excluídos.
- Sem mudança do esquema SQLite v5 e backup JSON schema 5.
- Build `:app:assembleDebug` e testes reais em celular ainda pendentes até GitHub Actions.
