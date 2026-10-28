# Finanças da Viagem 🚴

Aplicativo Android **0.1.0-alpha.4 / versionCode 4** para controle offline de finanças da cicloviagem. Kotlin + Jetpack Compose + Material 3 + SQLiteOpenHelper. Interface inspirada na referência enviada pelo usuário, com modo claro, escuro e sistema.

## Novidade na alpha.4: aba Recicláveis 🥫

- Navegação inferior **Recicláveis**, com estoque de **latinhas de alumínio** e **cobre** somente. Sem PET, papelão, ferro ou outros materiais no módulo de reciclagem, conforme a limitação de carga da bicicleta.
- **Coletar**, **vender** e **ajustar estoque** (ajustes obrigatoriamente explicados), com histórico e exclusão controlada. A exclusão de uma coleta que inviabilize venda posterior é recusada.
- Padrões ajustáveis no botão de parâmetros: **70 latinhas ≈ 1 kg** (estimativa fornecida pelo usuário) e **R$ 6,00 por kg**. O app jamais apresenta o peso de latinhas calculado como balança real. Uma pesagem real opcional prevalece no momento da venda.
- Cobre é registrado por **peso informado em kg**, sem conversão por contagem e sem preço padrão inventado; cada venda exige seu preço/kg.
- A coleta só aumenta o estoque. Uma **venda após recebimento** diminui o estoque e cria exatamente **uma receita na categoria Trabalho** na mesma transação SQLite; ela já integra o saldo geral. Não registrar a mesma venda pelo editor financeiro comum, para evitar receita duplicada.
- O valor potencial das latinhas em estoque é **estimativa não recebida**, separada do dinheiro real. O saldo financeiro não inclui materiais ainda não vendidos.
- Receitas originadas de vendas são protegidas contra edição/exclusão isolada no histórico financeiro: administre-as na aba Recicláveis. Exclusão de venda desfaz estoque e receita na mesma transação.
- **SQLite 3** com migrações aditivas 1→2→3 e 2→3, sem apagar dados. **Backup JSON schema 3** inclui eventos, vínculos com receitas e novos parâmetros, e continua importando backups schema 1/2. **CSV de reciclagem** separado do CSV financeiro.

## Funcionalidades implementadas

- Editor de lançamento redesenhado, compacto e responsivo: cabeçalho, alternância despesa/receita, valor destacado, detalhes opcionais expansíveis e botão Salvar fixo.
- O editor financeiro legado mantém a entrada opcional **Por quantidade**; vendas novas com controle de estoque devem ser feitas na aba **Recicláveis**. Os lançamentos existentes são mantidos; backups JSON v1 e v2 seguem aceitos.


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

**GitHub Actions**: publique esta pasta na raiz de um repositório no GitHub e execute **Actions → Gerar APK → Run workflow**. Workflow usa Gradle 8.13, JDK 17, `android-actions/setup-android@v4` e Android SDK 35, executa testes unitários e publica APK Debug em Artifacts em caso de sucesso. Se o build falhar, baixe o log, sem presumir sucesso.

> Este pacote **não inclui `gradle-wrapper.jar` nem um APK**. O workflow utiliza `gradle/actions/setup-gradle` para não exigir wrapper. O projeto fonte foi preparado para compilação, mas **não foi compilado no ambiente de geração** (sem Android SDK e Gradle/artefatos em cache). É preciso validar o novo build Android; os logs do build 1 mostraram falha antes da compilação em `setup-android@v3`, ao tentar instalar o pacote obsoleto `tools`. A alpha.2 corrigiu esse passo com v4 e `packages: platform-tools`.

## Privacidade e backup

O banco SQLite fica no diretório privado do aplicativo. Android Auto Backup é desativado. Backup/CSV são exportados pelo Storage Access Framework. Faça backup antes de desinstalar: limpar dados/desinstalar apaga os lançamentos. Restaurar um backup substitui o histórico e as configurações, após confirmação.

Limitações atuais: sem login, sem sincronização com contas bancárias, sem GPS, sem notificações, sem importação CSV, sem transferência entre contas, sem busca por texto e sem previsão automática de câmbio. O peso de alumínio derivado de quantidade é uma estimativa, não medição.

## Estrutura

- `app/src/main/java/.../core/FinanceMath.kt`: centavos, parsing, taxa e autonomia.
- `.../data/FinanceRepository.kt`: persistência SQLite, contas, estoque, vínculos de receita, backup e CSV.
- `.../ui`: tema, painel, histórico, edição, configurações e Sobre.
- `.github/workflows/android.yml`: testes e APK Debug via CI.
- `.../core/RecyclingMath.kt`: estimativas de peso, cálculo por kg e validações inteiras.
- `.../ui/RecyclingScreen.kt`: aba Recicláveis, formulário e parâmetros.
- `app/src/test`: testes de matemática monetária e de reciclagem.

## Versionamento

A cada alteração, incrementar versão `0.1.0-alpha.N`, `versionCode` e referências na documentação/CI. Nunca usar migração SQLite destrutiva em updates.

## Referência visual

A imagem enviada está preservada em `docs/layout_referencia.jpg` para orientar ajustes futuros. O layout é responsivo e os valores são calculados com dados reais, não copiados do desenho.

## Migrações históricas e alpha.4

O banco SQLite passa de v1 para v2 exclusivamente com três colunas opcionais (`item_kind`, `quantity`, `unit_minor`), sem apagar lançamentos antigos. Se houver dados de valor direto, as colunas ficam nulas. O backup JSON gerado usa schema 2, e restauração aceita schema 1 e 2.
