## 0.1.0-alpha.11 (versionCode 11) — paginação e consistência dos dados

- Histórico passa para paginação SQL de 60 itens, com filtros no banco, cursor estável por data/ID, carregamento incremental e opção de repetir consulta após falha.
- Formulários de Recicláveis separados em `RecyclingEditors.kt`, sem mudança de regras de negócio.
- SQLite **v6**: tabela `app_settings` adicionada; parâmetros existentes em SharedPreferences migrados sem excluir dados. Os parâmetros e movimentos agora são restaurados em uma única transação SQLite, removendo a divergência entre arquivos em falhas de restauração.
- Mantido backup **JSON schema 6** e importação de schemas 1–6; testes locais SQL de páginas e rollback, mais testes instrumentados para migração/restauração (pendentes de execução em dispositivo).
- VersionCode e workflow de APK sincronizados; build Android completo e teste de desempenho físico ainda pendentes.

## 0.1.0-alpha.10 (versionCode 10) — refatoração e estabilidade

- Extraídos `Models.kt`, `LedgerDb.kt` e `CsvExports.kt` de `FinanceRepository.kt`; sem mudanças no esquema persistido.
- Consultas de saldo por conta no SQLite, dentro das transações, substituindo releitura completa de todos os registros para validar transferências.
- Estados e refresh parcial separados em `FinanceUiStore.kt`, com controle de gerações por área e leitura fora da thread principal.
- Split de `Screens.kt` em histórico, ajustes/sobre e painel; listas lazy mantidas.
- Erros de saldo em Carteiras geram mensagem controlada em vez de mapa vazio seguido de exceção.
- Mantidos SQLite v5, backup JSON v6, UI/temas e interfaces antigas.
- Teste Python/SQLite em 100 cenários (16.000 operações), incluído na CI; teste instrumentado SQLite com 1.000 operações adicionado (execução em Android pendente). Validação estática e de lógica local; APK/benchmark em dispositivo pendentes.

## 0.1.0-alpha.9 (2026-10-30) • versionCode 9

- Reduzido trabalho de CPU nas telas longas: resumos do painel e de recicláveis consolidados em uma varredura e memorizações de totais, saldos e previsão.
- Listas lazy agora informam tipos de item; rolagem preservada entre abas usando `SaveableStateHolder`.
- Formatação monetária reutiliza `NumberFormat` em caches locais por thread, evitando criar formatadores em cada linha do histórico.
- Removidas varreduras redundantes em contas e planejamento durante interações e recomposições; leituras de banco desatualizadas não sobrescrevem snapshots recentes.
- Adicionados testes de equivalência do resumo e de integridade da matemática da reciclagem.
- Sem alteração de dados, SQLite v5, backup JSON schema 6, temas ou recursos. Build e testes de fluidez em aparelho ainda pendentes.

## 0.1.0-alpha.8 (2026-10-27) • versionCode 8

- Tela Planejamento: meta financeira, dias da cicloviagem, orçamento diário e limite de alerta (50 a 100%).
- Painel indica limite diário atingido ou aproximação configurada; alerta apenas local, sem push.
- Projeção de custo e insuficiência, autonomia e avanço da meta exclusivamente com saldo disponível real sem reserva, sem somar transferências nem materiais a vender.
- Backup JSON schema 6, compatível com restauração schema 1–5. SQLite v5 mantido, sem migração destrutiva.
- Sintaxe Kotlin e matemática local validadas; build do APK e teste em aparelho pendentes.

## 0.1.0-alpha.7 (2026-10-27) • versionCode 7

- Máscara de centavos e separador de milhar durante a digitação em campos monetários, sem alterar armazenamento inteiro.
- Campo `MoneyField` compartilhado com suporte a moedas sem casas decimais; não aplicar em cotação nem peso.
- Relatórios financeiros por período (7, 30 dias e tudo): receitas, despesas, resultado líquido, doações, trabalho/reciclagem, categorias e últimos sete dias.
- Mantidos SQLite v5, backup JSON schema 5 e mecanismos de carteiras, contas, reciclagem e migração das versões anteriores.
- Testes locais de formatação, matemática dos relatórios e parser Kotlin; build Android e teste em telefone pendentes.

## 0.1.0-alpha.6 (2026-10-27) • versionCode 6

- Botão **Usar todas** na venda de latinhas e cobre, preenchendo a quantidade em estoque sem vender automaticamente.
- Atalho **Doação** no painel, receita categorizada e saldo em carteira com resumo sem dupla contagem.
- Tela **Contas e despesas** acessível por Carteiras e pelo painel: contas a pagar com datas de vencimento, edição/exclusão de pendentes, pagamento ligado a uma despesa única, categorias e carteira de pagamento.
- Migração SQLite 4→5 aditiva, backup JSON schema 5 retrocompatível com 1/2/3/4 e exportação CSV de contas.
- Proteção contra edição manual de despesas vinculadas a pagamentos de contas.
- Corrigida duplicação visual do seletor de carteira no formulário de novo lançamento.
- Testes locais de sintaxe, core, datas e SQLite; compilação Android GitHub/execução em aparelho pendente.

# Alterações

## 0.1.0-alpha.5 (2026-10-27) • versionCode 5

- Aba Carteiras com Dinheiro físico, Banco / Pix, Reserva de emergência, novas contas e saldos iniciais.
- Lançamentos financeiros e recebimentos de reciclagem associados a carteiras.
- Transferências sem gerar receitas/despesas, com histórico, controles de saldo e CSV próprio.
- Migração SQLite 3→4 aditiva, cálculo por conta em centavos, dashboard exclui contas de reserva.
- Backup JSON schema 4 com carteiras e transferências, importação de v1/v2/v3/v4, preservação dos lançamentos históricos.
- Modo claro, escuro, configurações, recibos e funcionalidades da alpha.4 mantidos.
- Compilação Android e testes de upgrade em celular ainda pendentes.

## 0.1.0-alpha.4 (2026-10-27) • versionCode 4

- Nova aba inferior **Recicláveis** com apenas latinhas de alumínio e cobre, adequados ao transporte na bicicleta.
- Estoque por coletas, vendas e ajustes; histórico com exclusão segura. Ajustes exigem justificativa.
- Configuração inicial: **70 latinhas ≈ 1 kg** e **R$ 6,00/kg**, ambos ajustáveis. A estimativa é identificada; peso medido prevalece em vendas. Cobre usa peso informado e preço de venda manual.
- Venda confirmada gera **receita de Trabalho** e baixa estoque atomicamente, sem contar valor estimado como dinheiro; o histórico de finanças protege receitas vinculadas.
- Migração SQLite 1/2→3 aditiva, backup JSON schema 3 com vínculos de vendas e restauração v1/v2; CSV separado para movimentos de estoque.
- Testes locais de cálculos inteiros, SQL de migração e análise sintática. Build completo do Android e teste físico **ainda pendentes**.


## 0.1.0-alpha.3 (2026-10-27) • versionCode 3

- Redesenhado o editor de lançamentos com layout compacto, seções organizadas, local opcional e ação Salvar sempre disponível na base da janela.
- Adicionado registro opcional por quantidade: **Latinhas** ou **Outros itens**, preço unitário e total exato calculado automaticamente.
- Adicionadas categorias **Venda de latinhas** (receita) e **Bebidas** (despesa), com quantidade exibida no histórico.
- Banco SQLite migra de v1 para v2 sem destruição; backup JSON novo v2 e importação retrocompatível v1, exportação CSV com detalhes dos itens.
- Testes novos de multiplicação e limites; build Android/GitHub e validação visual em celular ainda pendentes.


## 0.1.0-alpha.2 (2026-10-27) • versionCode 2

- Corrigido o erro de inicialização do GitHub Actions identificado no build 1: `setup-android@v3` tentava instalar o pacote SDK `tools`, não disponível.
- Atualizado `android-actions/setup-android` para `@v4`, com `packages: platform-tools` (sem pacote obsoleto), e logs de licenças reduzidos.
- Mantidos Gradle 8.13, JDK 17, API 35 e Build Tools 35.0.0, coerentes com o AGP 8.11.1.
- Versionamento e nome do artifact sincronizados; os dados locais e o banco SQLite não foram alterados.
- **Pendente:** confirmar a compilação e os testes no GitHub Actions. A falha anterior ocorreu antes da etapa Gradle.


## 0.1.0-alpha.1 (2026-10-27) • versionCode 1

- Projeto Android inicial com painel fiel ao layout de referência e temas claro/escuro/sistema.
- Receitas, despesas, categorias, múltiplas moedas com câmbio manual, saldo, reserva, orçamento e estimativa de autonomia.
- Histórico com filtros, edição, exclusão e persistência privada SQLite.
- Exportação JSON/CSV e restauração JSON com confirmação.
- Tela Configurações e tela Sobre.
- Testes unitários da matemática e workflow GitHub Actions para APK Debug.
