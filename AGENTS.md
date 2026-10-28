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

