# Regras do projeto Finanças da Viagem

- Antes de cada mudança, inspecionar o código real, sem presumir recursos implementados.
- Incrementar `versionCode`, `versionName`, README, CHANGELOG e nome do artifact do workflow.
- Nunca usar `fallbackToDestructiveMigration` nem apagar dados em upgrades.
- Toda transação deve usar valores inteiros menores e conversão decimal exata; nunca dinheiro agregado em float/double.
- Conferir exportação/importação, UX offline e legibilidade de ambos os temas.
- Não adicionar permissões de internet ou armazenamento sem necessidade concreta.
- Ao entregar, distinguir testes locais, build GitHub e testes em aparelho.

- Dados de contagem de itens são opcionais; preservar migração 1→2 e a compatibilidade de backups v1/v2.
- Reciclagem: apenas `cans` e `copper`; nunca misturar material coletado/estimado com dinheiro recebido.
- Parâmetros iniciais são 70 latinhas/kg estimados e R$ 6,00/kg, ambos configuráveis. Cobre não tem preço implícito.
- Evento de venda e receita `Trabalho` vinculada devem ser criados ou excluídos na **mesma transação SQLite**. Proteger lançamentos vinculados no editor geral.
- Backup atual JSON schema 4: preservar importação de v1, v2 e v3; SQLite v4 com migração 1→2→3→4 aditiva.
- Toda receita/despesa deve apontar para conta válida. Transferências não criam receitas/despesas.
- Conta marcada como reserva não integra a autonomia; migração do saldo inicial legado não pode duplicar dinheiro.
- Ao adicionar nova conta, preservar as contas e transferências nos backups, CSV e restauração; transações devem permanecer inteiras em centavos.


- Backup JSON schema **5**: aceitar v1/v2/v3/v4; SQLite v5 com migração incremental 4→5, sem migração destrutiva.
- Contas a pagar: cadastramento não deve alterar saldo. Pagamento gera exatamente uma despesa na mesma transação SQLite e vincula `paid_entry_id`; bloquear edição/exclusão isolada de despesas vinculadas.
- Doações são receitas manuais e devem ser associadas a uma carteira; se a carteira é de reserva, não integram a autonomia.
- Estoque de recicláveis é derivado exclusivamente de `recycling_events`; nunca inferir estoque de outros lançamentos financeiros.

- Alpha.7: formatos monetários durante digitação são responsabilidade do MoneyField; não aplicar máscara de centavos em cotação cambial ou peso (kg).
- Relatórios usam apenas entries efetivos em BRL (brlCents); não somar transferências, estoque projetado ou saldos iniciais.
- SQLite permanece v5 e backup JSON schema 5; versionCode 7 e nome da CI alpha.7.

- Alpha.8: SQLite v5 inalterado, JSON schema **6** incluindo `tripGoalCents`, `plannedTripDays`, `budgetAlertPercent`; manter compatibilidade de backups 1–5 com defaults 0/0/80.
- Planejamento deriva somente do saldo das contas não-reserva; não misturar transferências, recicláveis não vendidos ou contas a pagar pendentes com dinheiro recebido.
- Sem notificações do sistema: avisos de limite aparecem no app; nunca prometer alertas em background.
- Versionar alpha.8 (versionCode 8), documentação, Sobre e nome do Artifact.

- Alpha.9: preservar SQLite v5 e backup JSON schema 6, versionCode 9. Manter resumos de UI em `ScreenSummaries` em centavos inteiros, memorizar cálculos dependentes de listas com `remember` e dar `contentType` aos itens `LazyColumn` homogêneos.
- `NumberFormat` é mutável e não thread-safe: usar cache local por thread, nunca singleton compartilhado entre threads.
- Estados de rolagem salvos por tela; não transformar listas lazy em `Column.verticalScroll` com centenas de itens.
- Perfis de fluidez reais dependem de medição no aparelho (jank/FrameTimeline); testes sintáticos não comprovam FPS.

- Alpha.10: `Models`, `LedgerDb`, `CsvExports` e `WalletBalanceQueries` isolados, API pública do repositório preservada.
- `FinanceUiStore` atualiza slices isoladas com geração individual por tipo e leitura fora da UI; não perder estado novo por carga assíncrona antiga.
- Não mostrar saldos como zero diante de falha de cálculo; apresentar aviso controlado. SQLite e JSON mantidos em v5/v6, sem migrações.

- Alpha.11 (versionCode 11): `LedgerDb` SQLite v6 com tabela única `app_settings` que recebe preferências legadas em migração 5→6; dados financeiros e configurações devem ser restaurados na mesma transação SQLite. Não gravar novas preferências financeiras em SharedPreferences.
- Manter o backup JSON schema 6 e compatibilidade importação 1–6. Histórico usa paginação `historyPage` por `(created_at DESC,id DESC)`, páginas de até 60, com filtros no SQL; não carregar todas as entradas para construir a tela Histórico.
- `RecyclingEditors.kt` concentra os formulários sem alterar regras de coleta/venda. Testes Android devem usar banco isolado; nunca excluir o banco de produção durante instrumentação.

- Alpha.12 (versionCode 12): compactação somente da UI inicial; botões Receita e Doação compartilham superfície, mas mantêm callbacks distintos. Planejar, Relatórios e Gerenciar têm atalhos individuais lado a lado e toque mínimo 72 dp. Não reduzir áreas clicáveis abaixo de 48 dp nem alterar regras financeiras.
- Preservar SQLite v6 e backup JSON schema 6. Testar modo claro/escuro e diferentes larguras antes de afirmar melhora visual comprovada no Android.

- 1.0.0 (versionCode 13): `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`; preservar `applicationId` e compatibilidade SQLite v6 / JSON v6.
- Para Google Play, gerar AAB Release assinado com os quatro Secrets existentes e Play App Signing; não inserir keystores/Secrets no repositório, artifacts, prints ou logs. Não gerar nova chave sem necessidade.
- Antes de declarar pronto para publicação, verificar builds, AAB assinado e Play Console, privacidade HTTPS pública, declaração de dados, recursos financeiros, imagem e screenshots; versão 1.0.0 não implica aprovação da Play.

- 1.0.1 (versionCode 14): o Painel exibe Ganhos hoje e Resultado de hoje, ambos derivados de receitas/despesas reais do dia local, excluindo saldo inicial e transferências. A seção Gastos por categoria mostra somente despesas. Manter autonomia na tela Planejamento.
- Artifact Release Play: `actions/upload-artifact@v7` com `archive: false` exige **um arquivo por step** e o GitHub usa o nome do arquivo como nome do Artifact. Testar integração com versões do GitHub Manager que esperam ZIP. Nunca incluir Secrets ou keystore no artifact.
