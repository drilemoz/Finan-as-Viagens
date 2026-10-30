# Fluidez e estabilidade da interface (alpha.9)

## Diagnóstico pelo código

A alpha.8 já utilizava `LazyColumn`. A lentidão durante a rolagem **não foi medida** em aparelho; não atribuir uma causa única sem rastreamento. A inspeção encontrou alocações de `NumberFormat` por valor, criação de `SimpleDateFormat` em alguns itens visíveis e múltiplos filtros/agrupamentos do mesmo histórico em caminhos de recomposição. Também não havia preservação do índice de rolagem ao trocar de aba.

## Alterações executadas

1. `ScreenSummaries`: resumo financeiro e reciclagem em uma passagem com `Math.addExact` / `Math.subtractExact`; não contar estoques estimados como receita.
2. `remember` com dependências explícitas nas telas para evitar recalcular totais e saldos quando um estado visual muda.
3. `LazyColumn` mantém virtualização, usa `contentType` para itens repetidos (lançamentos, eventos, transferências, contas) e `rememberLazyListState` salvo pelo `SaveableStateHolder` por tela.
4. `FinanceMath.format` reutiliza um formatador `NumberFormat` **por thread/moeda**; os resultados monetários e armazenamento inteiro permanecem inalterados.
5. `EntryRow`, eventos de reciclagem e transferências memorizam o texto de datas/valores.
6. `FinanceApp.refresh` descarta a resposta de uma leitura obsoleta quando outra consulta mais nova já foi iniciada.

## Testar em aparelho

- Faça backup JSON antes de atualizar. Nunca limpe os dados do aplicativo para fazer teste de velocidade.
- Use a mesma base de dados e o mesmo aparelho para comparar alpha.8 e alpha.9. Teste Painel, Histórico, Recicláveis, Carteiras, Planejamento e Contas a pagar, com e sem teclado aberto.
- Registre as travadas observadas e, se tiver ADB, use `adb shell dumpsys gfxinfo br.com.adriedson.financasdaviagem reset`, reproduza uma rolagem fixa, depois `adb shell dumpsys gfxinfo br.com.adriedson.financasdaviagem framestats`. Não compare resultados obtidos com builds de depuração/otimização diferentes.
- Confirme que o início/fim da lista e a posição ao alternar abas não se perdem e que lançamentos/vendas/doações alteram os valores após salvar.

**Limitações:** não foram coletadas métricas de FPS/jank e não houve compilação Gradle ou teste Android real nesta preparação. As verificações de Kotlin, formatação e matemática não comprovam ganho de desempenho no dispositivo.


## Alpha.10 — refatoração e benchmark pendente
- Os carregamentos após mudanças de conta ou transferência são específicos por domínio, em vez de fazer 6 leituras completas.
- As verificações de transferências consultam `SUM` e saldo de uma conta no SQLite, sem materializar listas completas. Validações matemáticas continuam usando inteiros.
- A tela de Carteiras não transforma falha de cálculo em saldo zero.
- Não há melhora de FPS quantificada. Perfis com 100/1000/5000 entradas e compilação real ainda são necessários.
- Alpha.11: a restauração JSON agora usa uma transação SQLite única para dados e configurações (tabela `app_settings` em SQLite v6). Ainda faça backup externo antes de restaurar, pois a importação substitui os registros atuais.

## Alpha.11: validação de rolagem

- O Histórico deixou de usar `entries.filter` para criar a lista inteira e recebe páginas de até 60 registros do SQLite em ordem `(created_at DESC,id DESC)`.
- Testar no aparelho com 100, 1.000 e 5.000 lançamentos, incluindo muitos com mesmo timestamp, filtros Hoje/7 dias/30 dias e alteração de período durante a rolagem.
- Medir jank (FrameTimeline/Perfetto) e uso de RAM em 1.000+ lançamentos; testes SQL não comprovam FPS.
- A inicialização e o Painel ainda podem consultar o histórico completo para seus agregados; otimizar essas consultas em futuras versões conforme medições.
