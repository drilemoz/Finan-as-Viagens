# Finanças da Viagem 🚴

Aplicativo Android **1.0.1 / versionCode 14** para controle offline de finanças da cicloviagem. Kotlin + Jetpack Compose + Material 3 + SQLiteOpenHelper. Interface inspirada na referência enviada pelo usuário, com modo claro, escuro e sistema.

## Versão 1.0.1: ganhos do dia e arquivos AAB diretos

- Painel: **Ganhos hoje** substitui **Dias de autonomia**; o indicador soma apenas receitas registradas no dia local, incluindo vendas confirmadas de recicláveis. Uma linha **Resultado de hoje** apresenta receitas menos despesas do dia em centavos inteiros, sem incluir saldo inicial e transferências entre carteiras. **Gastos por categoria** continua contendo apenas despesas (sem subtrair ganhos).
- O cálculo da autonomia permanece na tela **Planejamento da viagem**, mas não ocupa mais um cartão do painel.
- GitHub Actions: workflow Play publica **dois Artifacts individuais**, `.aab` e `.apk`, usando `actions/upload-artifact@v7` com `archive: false` (sem ZIP externo). Isso não altera nem substitui o comportamento de Artifacts antigos; o GitHub Manager poderá depender de suporte à nova modalidade na interface de download.
- `versionName 1.0.1`, `versionCode 14`; preservados package, API 36, SQLite v6 e JSON v6. Não inserir certificados e Secrets no repositório.

## Versão 1.0.0: preparação Google Play (Android 16)

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` e `versionCode = 14` na versão atual; sem troca do pacote Android.
- Geração de **AAB Release assinado**, além de APK Release para instalação manual, por `.github/workflows/android-play.yml` no GitHub Actions, usando os mesmos quatro Secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD` já criados no GitHub Manager. O certificado é usado como **chave de upload**; a assinatura dos APKs distribuídos pela loja deve ser gerenciada pelo Google Play App Signing. Nunca commitar `.env`, `.p12`, `.jks` ou keystore.
- Compatibilidade com telas ponta a ponta no Android 16: `enableEdgeToEdge()` e ajuste de teclado (`adjustResize`), com ícones das barras claros/escuros conforme o tema. A interface deve ser validada no Android 16 antes de enviar.
- Política de privacidade dentro do app em Ajustes e Sobre, com rascunho público em `docs/privacy-policy.html` **a ser hospedado em URL HTTPS pública**. Sem novo uso de rede, localização ou coleta de dados.
- Novo `docs/GOOGLE_PLAY.md`: exigências, imagens, conteúdo e revisão da loja; `docs/PLAY_LISTING.md`: descrição proposta. Ícone PNG de 512px derivado do ícone original em `docs/play-assets/`, sujeito a revisão de qualidade.
- SQLite v6, backup JSON schema 6 e regras financeiras preservados. Sem migração de dados nem alteração de operações monetárias.
- **Pendente:** compilação Android real/AAB no GitHub Actions, teste de API 36 e 16 KB, vínculo e verificação de política HTTPS, ficha e declarações Play Console, teste fechado caso seja exigido. Preparado para submissão, não publicado.

## Alpha.12: painel mais compacto (somente interface)

- Cartão de saldo, cartões de indicadores e estado vazio com margens e espaçamentos menores **somente no Painel**, preservando tipografia legível e cartões de Planejamento.
- Botão primário **Adicionar despesa** mais baixo; botões **Receita** e **Doação** reunidos em um mesmo contêiner dividido, preservando ações separadas.
- **Planejar**, **Relatórios** e **Gerenciar** lado a lado com ícones, textos curtos e áreas de toque mínimas de 72 dp; mantidos os mesmos destinos e callbacks.
- `ValueCard` e `EmptyState` aceitam `compact = true` apenas na tela inicial; restante das telas não muda.
- Nenhuma mudança no SQLite **v6**, backup JSON schema **6**, cálculos, funcionalidades, permissões ou necessidade de migração. Validação de compilação e ergonomia em celular permanece pendente.

## Alpha.11: histórico paginado e backup transacional

- Histórico consulta o SQLite por **lotes de 60 lançamentos**, aplicando filtros e cursor estável `(created_at, id)` no próprio SQL. O próximo lote é carregado ao rolar; em caso de erro, há botão de tentar novamente. A tela não filtra mais a lista inteira na composição.
- Os formulários de coleta, venda, ajuste e parâmetros foram movidos de `RecyclingScreen.kt` para `RecyclingEditors.kt`, preservando as regras e o design anteriores.
- Configurações deixam de ser atualizadas em SharedPreferences: uma **migração SQLite aditiva 5→6** importa os parâmetros existentes para `app_settings` e torna restauração de lançamentos e preferências **uma transação SQLite única**. Arquivos de backup JSON continuam no **schema 6**, aceitando 1–6.
- Um banco de teste isolado é usado nos testes instrumentados, sem apagar o banco financeiro real. Testes de rolagem, restauração e upgrade em aparelho ainda são pendentes.
- Nenhuma permissão nova, novo módulo financeiro ou mudança das operações de reciclagem.

## Alpha.10: refatoração de estabilidade (sem novas funções)

- Separação de modelos financeiros, criação/migração SQLite, exportações CSV e cálculo do saldo individual das carteiras em componentes próprios, preservando nomes de métodos e dados existentes.
- Novo `FinanceUiStore`: leituras em `Dispatchers.IO`, atualização somente das áreas afetadas e descarte de respostas antigas por geração individual. Uma transferência não relê reciclagem, contas a pagar e todo o histórico.
- Transferências verificam o saldo da conta com consulta SQLite `SUM` na transação ativa, evitando materializar listas completas de lançamentos/transferências para validar cada operação.
- Carteiras exibe aviso de erro em vez de cair por usar mapa vazio se o cálculo falhar.
- História e Ajustes/Sobre organizados em arquivos próprios. `LazyColumn` e temas preservados.
- **Sem migração:** SQLite v5 e JSON v6; nenhuma alteração de tabelas, nomes de chaves ou IDs. Não remover dados nem desinstalar antes do backup.
- Teste SQL com 16.000 operações incluído em `tools/verify_refactor.py` e no GitHub Actions. Teste instrumentado Android de saldo SQL com 1.000 operações incluído, mas ainda não executado em emulador ou aparelho. Não há medição de FPS; testar rolagem longa com milhares de lançamentos e repetir após APK de CI.

## Novidades na alpha.9: fluidez de rolagem e estabilidade

- Painel: receitas, despesas, doações, total diário e categorias em **uma passagem** pelo histórico, calculados apenas após mudança nos dados. Saldo por conta também é memorizado; não é recalculado quando a tela apenas recompõe.
- Recicláveis: resumo de estoque, coletas, vendas, cobre e pagamentos consolidado em **uma passagem** pelos eventos e reaproveitado até ocorrerem alterações.
- Carteiras, contas a pagar e planejamento: nomes, totais, saldos e despesas do dia não são varridos novamente a cada interação do formulário.
- Histórico/recicláveis/carteiras/contas: `LazyColumn` com tipos de linha explícitos para melhor reutilização de itens; rolagem preservada entre telas via `SaveableStateHolder`.
- Moedas: `NumberFormat` reutilizado **por thread e por moeda**, sem acessar formatadores mutáveis entre threads. Dados persistidos continuam inteiros e cálculos não mudaram.
- Nenhuma permissão adicional, migração SQLite ou mudança de backup (SQLite v5, JSON schema 6). Testar especialmente em aparelho físico e usando listas longas. Veja `docs/PERFORMANCE.md` para diagnóstico e roteiro de medição.

## Novidades na alpha.8: planejamento da cicloviagem 🚴📅

- Nova tela **Planejamento da viagem**, acessível no Painel e em Configurações, com meta financeira (valor em BRL), orçamento diário e dias planejados (0 = indefinido).
- Mostra **saldo utilizável real**, sem reserva, com receitas já recebidas (incluindo vendas confirmadas e doações), sem contar coleta ou vendas projetadas como renda.
- Calcula **custo dos dias planejados** (`orçamento diário × dias`), **diferença entre meta e saldo**, **falta para custear os dias** e **autonomia** (`saldo utilizável ÷ orçamento diário`) em centavos inteiros. Nunca presume dinheiro futuro.
- Alerta **dentro do aplicativo** quando o gasto efetivo do dia atinge percentual configurável (50/70/80/90/100%) ou ultrapassa o limite diário. Não existem notificações do sistema nem serviço em background.
- Parâmetros armazenados localmente em SharedPreferences; **SQLite continua na versão 5**, sem migração e sem alteração dos lançamentos.
- **Backup JSON schema 6** inclui meta, dias e limite de alerta. Restauração ainda aceita backups das versões **1 a 5** com os novos parâmetros inicialmente desativados; exportação CSV e demais módulos inalterados.
- Meta e projeção são estimativas de planejamento, não garantias de duração. O peso estimado das latinhas e o cobre não vendido não integram saldo disponível.

## Novidades na alpha.7: valores formatados e relatórios

- **Digitação monetária automática**: em valores R$, digitar `1`, `2`, `3`, `4`, `5`, `6` apresenta sequencialmente `0,01`, `0,12`, `1,23`, `12,34`, `123,45` e `1.234,56`. O texto exibido possui pontuação, mas o parser recebe a forma canônica sem separador de milhares; o banco continua gravando `Long` em centavos.
- `MoneyField` aplicado a receitas/despesas, preço por item, orçamento diário, saldos iniciais, transferência, conta a pagar, preço de venda e preço estimado por kg de recicláveis. Moedas sem frações (`PYG` e `CLP`) recebem apenas separador de milhar. **Cotações cambiais e pesos em kg NÃO são tratados como dinheiro** e mantêm seus próprios formatos.
- **Relatórios** acessíveis pelo Painel: filtros de 7 dias, 30 dias e todo o histórico; entradas efetivas, saídas, resultado líquido, doações, receita de trabalho/reciclagem, categorias e evolução diária dos últimos sete dias. Coletas não vendidas, transferências e saldo inicial não são receitas.
- Sem alterar a versão do banco SQLite (segue **v5**) ou o backup JSON (segue **schema 5**). Tudo é calculado a partir dos lançamentos existentes. Sem dependência de internet.

## Novidade na alpha.6: Usar todas, Doações e Contas a pagar

- Na aba **Recicláveis > Vender**, o botão **Usar todas** preenche de uma vez o estoque atual de **latinhas** ou de **cobre** no campo de quantidade/peso. O cálculo usa 70 latinhas/kg e R$ 6,00/kg como parâmetros *estimados* e editáveis para latinhas; o cobre exige preço por kg informado. A venda só aumenta o dinheiro disponível após confirmar o recebimento, e não deve ser duplicada como receita manual.
- A tela de venda já mostrava `Estoque: N latinhas` porque lia `recycling_events` (coletas menos vendas e ajustes). **Lançamentos financeiros manuais de latinhas não alteram esse estoque**, evitando duplicações e suposições sobre compras/vendas antigas.
- O painel ganhou ação rápida **Doação**, que abre lançamento do tipo `Receita` com categoria **Doação** pré-selecionada. É possível escolher carteira (dinheiro físico/Banco Pix etc.). Doações recebidas integram saldo disponível, exceto quando depositadas na conta de reserva. Resumo de doações no painel sem somá-las duas vezes.
- A aba **Carteiras** e o painel oferecem acesso a **Contas a pagar**: cadastrar/editar/excluir contas pendentes com vencimento, categoria, descrição e valor; listar pendentes, pagas e vencidas; escolher carteira ao confirmar pagamento. Pagamento gera **uma despesa vinculada** na mesma transação SQLite e não permite pagar duas vezes. Contas pagas são preservadas no histórico e não podem ser excluídas isoladamente.
- **SQLite v5**: migração aditiva 4→5, cria tabela `bills`; versões 1/2/3 também migram sucessivamente. **Backup JSON schema 5** inclui contas a pagar e aceita os backups 1–4, em que contas a pagar começam vazias. Exportação CSV independente para contas a pagar.
- Corrigida duplicidade do seletor de carteira no formulário geral. Continuam disponíveis temas claro, escuro e do sistema, uso offline e cálculos monetários em centavos inteiros.

## Novidade na alpha.5: carteiras e contas 💰

- Nova aba **Carteiras** no menu inferior: **Dinheiro físico**, **Banco / Pix** e **Reserva de emergência** como contas iniciais, com nomes e saldos iniciais editáveis; também é possível criar outras contas.
- Cada receita/despesa pertence a uma conta escolhida no editor. A venda de recicláveis permite selecionar a conta onde o pagamento foi recebido, permanecendo vinculada à receita única de **Trabalho**.
- **Transferências** entre contas com validação de valor e saldo, histórico e exportação CSV. Transferir não cria receita/despesa e não muda o total de dinheiro. A reserva fica fora da autonomia, mas pode receber ou enviar transferências explícitas.
- A migração SQLite **3→4** preserva o histórico: lançamentos anteriores são atribuídos ao dinheiro físico. O antigo saldo inicial vira o saldo de abertura das contas, separando a reserva sem duplicar dinheiro. Se a antiga reserva era maior que o saldo inicial, a conta principal pode iniciar negativa, mantendo a aritmética original.
- **Backup JSON schema 4** inclui contas e transferências e aceita restauração dos formatos v1, v2, v3 e v4. CSV financeiro inclui a identificação da conta, e há CSV para transferências.
- Valores armazenados em **centavos inteiros**, sem APIs bancárias, internet, sincronização automática ou conexão com Pix.

## Novidade na alpha.4: aba Recicláveis 🥫

- Navegação inferior **Recicláveis**, com estoque de **latinhas de alumínio** e **cobre** somente. Sem PET, papelão, ferro ou outros materiais no módulo de reciclagem, conforme a limitação de carga da bicicleta.
- **Coletar**, **vender** e **ajustar estoque** (ajustes obrigatoriamente explicados), com histórico e exclusão controlada. A exclusão de uma coleta que inviabilize venda posterior é recusada.
- Padrões ajustáveis no botão de parâmetros: **70 latinhas ≈ 1 kg** (estimativa fornecida pelo usuário) e **R$ 6,00 por kg**. O app jamais apresenta o peso de latinhas calculado como balança real. Uma pesagem real opcional prevalece no momento da venda.
- Cobre é registrado por **peso informado em kg**, sem conversão por contagem e sem preço padrão inventado; cada venda exige seu preço/kg.
- A coleta só aumenta o estoque. Uma **venda após recebimento** diminui o estoque e cria exatamente **uma receita na categoria Trabalho** na mesma transação SQLite; ela já integra o saldo geral. Não registrar a mesma venda pelo editor financeiro comum, para evitar receita duplicada.
- O valor potencial das latinhas em estoque é **estimativa não recebida**, separada do dinheiro real. O saldo financeiro não inclui materiais ainda não vendidos.
- Receitas originadas de vendas são protegidas contra edição/exclusão isolada no histórico financeiro: administre-as na aba Recicláveis. Exclusão de venda desfaz estoque e receita na mesma transação.
- **SQLite 5** com migrações aditivas 1→2→3→4→5, sem apagar dados. **Backup JSON schema 5** inclui eventos, vínculos com receitas, contas, transferências e parâmetros, e continua importando backups schema 1/2/3/4. **CSV de reciclagem** separado do CSV financeiro.

## Funcionalidades implementadas

- Editor de lançamento redesenhado, compacto e responsivo: cabeçalho, alternância despesa/receita, valor destacado, detalhes opcionais expansíveis e botão Salvar fixo.
- O editor financeiro legado mantém a entrada opcional **Por quantidade**; vendas novas com controle de estoque devem ser feitas na aba **Recicláveis**. Os lançamentos existentes são mantidos; backups JSON v1 e v2 seguem aceitos.


- Painel: saldo disponível, orçamento diário, gasto de hoje, estimativa de dias de autonomia, gastos totais e distribuição por categoria.
- Cadastro de **despesas e receitas**, com categorias, descrição, cidade e país opcionais.
- Histórico com filtros por tipo e período, edição e exclusão com confirmação.
- Saldos iniciais e reserva na aba **Carteiras**; orçamento diário em **Configurações**.
- Modo claro, modo escuro e acompanhar tema do sistema.
- Moedas BRL, ARS, PYG, UYU, BOB, CLP e PEN, **conversão manual** para BRL por lançamento; nenhuma cotação automática.
- Backup JSON com validação/confirmar antes de substituir e exportação CSV pelo seletor de arquivos Android.
- Tela Sobre, dados locais privados, nenhuma permissão de internet.

## Comportamento financeiro

- Todos os valores agregados são em **centavos de real**, sem `Float/Double` na contabilidade; moedas sem centavos (PYG e CLP) usam unidade inteira.
- `Saldo disponível = soma dos saldos de todas as contas não marcadas como reserva`; cada conta começa no saldo inicial e recebe os lançamentos e transferências atribuídos a ela.
- `Autonomia estimada (dias inteiros) = saldo disponível / orçamento diário`, quando ambos > 0. Não é previsão de consumo real.
- Ao iniciar, os totais são zerados. O orçamento diário padrão é R$ 40,00, editável; **não são criados lançamentos fictícios**.
- Configure a abertura de cada carteira conforme o dinheiro real existente no instante inicial do controle. Não registre o mesmo dinheiro como receita, pois seria duplicado.
- A reserva é uma conta marcada como protegida: o saldo dela não compõe o dinheiro utilizável. Transferências explícitas podem mover valores para dentro ou fora da reserva.
- O aplicativo armazena a taxa manual por transação. Caso dinheiro seja convertido de moeda, use a cotação efetiva. Não há atualização retroativa das taxas.

## Compilar

**Android Studio**: abra esta pasta como projeto Gradle, configure JDK 17 e SDK Android 36, execute **Sync Project with Gradle Files** e `app > Tasks > build > assembleDebug`.

**GitHub Actions**: publique esta pasta na raiz de um repositório no GitHub e execute **Actions → Gerar APK → Run workflow**. Workflow usa Gradle 8.13, JDK 17, `android-actions/setup-android@v4` e Android SDK 36, executa testes unitários e publica APK Debug em Artifacts em caso de sucesso. Se o build falhar, baixe o log, sem presumir sucesso.

> Este pacote **não inclui `gradle-wrapper.jar` nem um APK**. O workflow utiliza `gradle/actions/setup-gradle` para não exigir wrapper. O projeto fonte foi preparado para compilação, mas **não foi compilado no ambiente de geração** (sem Android SDK e Gradle/artefatos em cache). É preciso validar o novo build Android; os logs do build 1 mostraram falha antes da compilação em `setup-android@v3`, ao tentar instalar o pacote obsoleto `tools`. A alpha.2 corrigiu esse passo com v4 e `packages: platform-tools`.

## Privacidade e backup

O banco SQLite fica no diretório privado do aplicativo. Android Auto Backup é desativado. Backup/CSV são exportados pelo Storage Access Framework. Faça backup antes de desinstalar: limpar dados/desinstalar apaga os lançamentos. Restaurar um backup substitui o histórico e as configurações, após confirmação.

Limitações atuais: sem login, sem sincronização com contas bancárias, sem GPS, sem notificações, sem importação CSV, sem busca por texto e sem previsão automática de câmbio. O peso de alumínio derivado de quantidade é uma estimativa, não medição.

## Estrutura

- `app/src/main/java/.../core/FinanceMath.kt`: centavos, parsing, taxa e autonomia.
- `.../data/FinanceRepository.kt`: operações financeiras, paginação SQL, contas, estoque e backup; `LedgerDb.kt`: estrutura e migração SQLite; `RecyclingEditors.kt`: formulários de reciclagem.
- `.../ui`: tema, painel, histórico, edição, configurações e Sobre.
- `.github/workflows/android.yml`: testes e APK Debug via CI.
- `.../core/RecyclingMath.kt`: estimativas de peso, cálculo por kg e validações inteiras.
- `.../core/WalletMath.kt`: saldos por carteira e transferências sem duplicar renda.
- `.../ui/WalletScreen.kt`: carteiras, edição de saldos iniciais, transferência e histórico.
- `.../ui/RecyclingScreen.kt`: aba Recicláveis, formulário e parâmetros.
- `app/src/test`: testes de matemática monetária e de reciclagem.

## Versionamento

A cada alteração, incrementar `versionCode` e versionar `versionName`, referências de Sobre, README, CHANGELOG, scripts e nomes dos artifacts CI. Nunca usar migração SQLite destrutiva em updates.

## Referência visual

A imagem enviada está preservada em `docs/layout_referencia.jpg` para orientar ajustes futuros. O layout é responsivo e os valores são calculados com dados reais, não copiados do desenho.

## Migrações históricas e alpha.6

O banco SQLite passa de v1 para v2 exclusivamente com três colunas opcionais (`item_kind`, `quantity`, `unit_minor`), sem apagar lançamentos antigos. Se houver dados de valor direto, as colunas ficam nulas. Na alpha.2, o backup JSON usava schema 2. A versão atual gera schema 6 e continua aceitando schemas 1, 2, 3, 4 e 5.

### SQLite v4 e backups

Migração aditiva 3→4 cria contas, transferências e adiciona `account_id` ao histórico. Ao migrar, dinheiro físico recebe o antigo saldo inicial menos a reserva, a reserva recebe o valor reservado e Banco / Pix inicia zerado. Todas as entradas históricas ficam atribuídas à conta Dinheiro físico. Backups antigos v1/v2/v3 são convertidos para esse modelo durante a restauração. Não desinstale antes de validar backup.

### SQLite v5 e contas a pagar

Migração 4→5 cria `bills` e índice por vencimento, sem alterações destrutivas em carteiras, entradas e reciclagem. A despesa da conta só é criada no pagamento confirmado e vinculada por `paid_entry_id` único. O histórico não permite editar ou apagar uma despesa vinculada; contas pagas continuam registradas. Para alterar uma conta já paga, o procedimento seguro de estorno deverá ser definido em versão futura. Backup schema 5 inclui esse vínculo e valida referência/valor antes de restaurar.
