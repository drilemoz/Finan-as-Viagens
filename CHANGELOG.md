# Alterações

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
