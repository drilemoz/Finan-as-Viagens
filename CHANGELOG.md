# Alterações

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
