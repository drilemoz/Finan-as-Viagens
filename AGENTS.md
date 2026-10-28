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
- Manter backup JSON schema 3 e compatibilidade com v1/v2, migração SQLite 1→2→3 aditiva e exportação CSV operacional.

