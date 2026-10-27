# Finanças da Viagem 🚴

Aplicativo Android **0.1.0-alpha.1 / versionCode 1** para controle offline de finanças da cicloviagem. Kotlin + Jetpack Compose + Material 3 + SQLiteOpenHelper. Interface inspirada na referência enviada pelo usuário, com modo claro, escuro e sistema.

## Funcionalidades implementadas

- Painel: saldo disponível, orçamento diário, gasto de hoje, estimativa de dias de autonomia, gastos totais e distribuição por categoria.
- Cadastro de **despesas e receitas**, com categorias, descrição, cidade e país opcionais.
- Histórico com filtros por tipo e período, edição e exclusão com confirmação.
- Saldo inicial, orçamento diário e reserva de emergência em Configurações.
- Modo claro, modo escuro e acompanhar tema do sistema.
- Moedas BRL, ARS, PYG, UYU, BOB, CLP e PEN, **conversão manual** para BRL por lançamento; nenhuma cotação automática.
- Backup JSON com validação/confirmar antes de substituir e exportação CSV pelo seletor de arquivos Android.
- Tela Sobre, dados locais privados, nenhuma permissão de internet.

## Comportamento financeiro

- Todos os valores agregados são em **centavos de real**, sem `Float/Double` na contabilidade; moedas sem centavos (PYG e CLP) usam unidade inteira.
- `Saldo disponível = saldo inicial + receitas - despesas - reserva de emergência`.
- `Autonomia estimada (dias inteiros) = saldo disponível / orçamento diário`, quando ambos > 0. Não é previsão de consumo real.
- Ao iniciar, os totais são zerados. O orçamento diário padrão é R$ 40,00, editável; **não são criados lançamentos fictícios**.
- O saldo inicial deve ser o dinheiro total no instante inicial do controle. Não lançar o mesmo valor como receita, para evitar contagem dupla.
- A reserva é apenas uma parcela protegida do saldo, não uma transação bancária.
- O aplicativo armazena a taxa manual por transação. Caso dinheiro seja convertido de moeda, use a cotação efetiva. Não há atualização retroativa das taxas.

## Compilar

**Android Studio**: abra esta pasta como projeto Gradle, configure JDK 17 e SDK Android 35, execute **Sync Project with Gradle Files** e `app > Tasks > build > assembleDebug`.

**GitHub Actions**: publique esta pasta na raiz de um repositório no GitHub e execute **Actions → Gerar APK → Run workflow**. Workflow usa Gradle 8.13, JDK 17 e Android SDK 35, executa testes unitários e publica APK Debug em Artifacts. Se o build falhar, baixe o log, sem presumir sucesso.

> Este pacote **não inclui `gradle-wrapper.jar` nem um APK**. O workflow utiliza `gradle/actions/setup-gradle` para não exigir wrapper. O projeto fonte foi preparado para compilação, mas **não foi compilado no ambiente de geração** (sem Android SDK e Gradle/artefatos em cache). É preciso validar o primeiro build Android.

## Privacidade e backup

O banco SQLite fica no diretório privado do aplicativo. Android Auto Backup é desativado. Backup/CSV são exportados pelo Storage Access Framework. Faça backup antes de desinstalar: limpar dados/desinstalar apaga os lançamentos. Restaurar um backup substitui o histórico e as configurações, após confirmação.

Limitações da alpha.1: sem login, sem sincronização com contas bancárias, sem GPS, sem notificações, sem importação CSV, sem transferência entre contas, sem busca por texto e sem previsão automática de câmbio.

## Estrutura

- `app/src/main/java/.../core/FinanceMath.kt`: centavos, parsing, taxa e autonomia.
- `.../data/FinanceRepository.kt`: persistência SQLite, configurações, backup e CSV.
- `.../ui`: tema, painel, histórico, edição, configurações e Sobre.
- `.github/workflows/android.yml`: testes e APK Debug via CI.
- `app/src/test`: testes de matemática monetária.

## Versionamento

A cada alteração, incrementar versão `0.1.0-alpha.N`, `versionCode` e referências na documentação/CI. Nunca usar migração SQLite destrutiva em updates.

## Referência visual

A imagem enviada está preservada em `docs/layout_referencia.jpg` para orientar ajustes futuros. O layout é responsivo e os valores são calculados com dados reais, não copiados do desenho.
