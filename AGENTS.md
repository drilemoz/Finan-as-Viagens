# Regras do projeto Finanças da Viagem

- Antes de cada mudança, inspecionar o código real, sem presumir recursos implementados.
- Incrementar `versionCode`, `versionName`, README, CHANGELOG e nome do artifact do workflow.
- Nunca usar `fallbackToDestructiveMigration` nem apagar dados em upgrades.
- Toda transação deve usar valores inteiros menores e conversão decimal exata; nunca dinheiro agregado em float/double.
- Conferir exportação/importação, UX offline e legibilidade de ambos os temas.
- Não adicionar permissões de internet ou armazenamento sem necessidade concreta.
- Ao entregar, distinguir testes locais, build GitHub e testes em aparelho.

- Dados de contagem de itens são opcionais; preservar migração 1→2 e a compatibilidade de backups v1/v2.
