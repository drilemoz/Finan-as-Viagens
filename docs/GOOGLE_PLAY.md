# Checklist de publicação | Finanças da Viagem 1.0.1

## Identidade técnica

- `applicationId`: `br.com.adriedson.financasdaviagem` (não trocar após publicar).
- `versionName`: `1.0.1`, `versionCode`: `14` (próximas atualizações com código maior).
- `compileSdk = 36` e `targetSdk = 36`, `minSdk = 26`.
- Android nativo Kotlin/Compose; app offline; sem `INTERNET`, localização, contatos ou permissão de armazenamento ampla.
- SQLite v6 e backups JSON schema v6. Antes de testes de upgrade/desinstalação: exportar backup por Ajustes e verificar leitura.

## Como obter o AAB de upload

1. Garanta que os Secrets GitHub `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` estejam configurados corretamente no repositório. **Não** copie `.env`, `.p12` ou `.jks` para dentro do projeto.
2. Envie o código-fonte para GitHub, mantendo `.github/workflows/android-play.yml`.
3. No GitHub Actions, execute **Google Play - AAB Release assinado** por `workflow_dispatch`.
4. O job instala SDK 36, roda verificações, `testDebugUnitTest`, `bundleRelease` e `assembleRelease`; verifica `jarsigner` do AAB e `apksigner` do APK. Com `upload-artifact@v7` e `archive: false`, fornece **dois Artifacts sem ZIP**: `Financas-da-Viagem-v1.0.1-Play.aab` e `Financas-da-Viagem-v1.0.1-Release.apk`. Versões antigas de Artifacts continuam compactadas.
5. O arquivo `Financas-da-Viagem-v1.0.1-Play.aab` é o bundle para enviar na Play Console (não o ZIP do código e não o APK Debug). Esse AAB deve ser assinado com a **chave de upload**. Ative Play App Signing na Play Console; o Google mantém a chave de assinatura distribuída aos usuários.
6. Verifique se o primeiro cadastro do pacote e seu certificado são os esperados; futuras atualizações exigem o mesmo ID de pacote e upload key cadastrada e versionCode crescente.

## Privacidade e declarações na Play Console (pendentes)

- Hospede `docs/privacy-policy.html` como **página pública HTTPS**, visível globalmente e sem login, por exemplo via GitHub Pages. **Não há URL pública configurada automaticamente neste projeto**. Insira esse URL na ficha da loja. A política pode ser lida também em **Ajustes > Política de privacidade**.
- Confirme, no AAB final e em todos SDKs de terceiros, se o app realmente não coleta, compartilha ou transmite dados ao desenvolvedor; então responda à seção *Segurança de dados* de acordo com a implementação e a redação da política. Os backups CSV/JSON enviados pelo usuário por outros aplicativos são de responsabilidade do usuário e podem conter dados sensíveis.
- Preencha a declaração de *Recursos financeiros*, exigida para apps da loja. O app administra registros e simula transferências entre carteiras **locais**; não executa pagamentos, Pix, banco ou investimentos. Responda conforme a definição e opções atuais exibidas na Play Console (categoria 'Outros', se cabível), não se apresente como banco.
- Informe classificação indicativa, público-alvo, categoria e acesso ao aplicativo. Não há login nem credenciais de testes necessários.
- Informe contato de suporte do desenvolvedor na Play Console. Nunca inventar email de suporte.
- Envie capturas de tela **reais** do app sem expor dados financeiros. Inclua ao menos duas imagens de celular e cumpra as especificações atuais da loja.
- Envie ícone de alta resolução 512x512 e gráfico de destaque 1024x500. `docs/play-assets/icone-play-512.png` é adaptação por ampliação do ícone atual e deve ser revisado para qualidade antes do envio. Gráfico de destaque e screenshots não estão incluídos.
- Se sua conta pessoal de desenvolvedor foi criada depois de 13 de novembro de 2023, realize teste fechado com ao menos 12 testadores inscritos continuamente por 14 dias antes de solicitar acesso à produção, conforme exigência atual.
- Para dispositivos Android 15+ verifique no AAB final bibliotecas nativas (`.so`) e compatibilidade com páginas de 16 KB; não inferir de fonte apenas.

## Testes antes de produção

- Gerar AAB assinado e verificar no Play Console que foi aceito.
- Rodar testes instrumentados SQLite (`connectedAndroidTest`) em emulador ou dispositivo isolado, nunca em banco real de produção.
- Testar upgrade 1.0.0 → 1.0.1 **com assinatura compatível** e sem perda dos lançamentos; testar também instalação via teste interno Play, que pode usar chave distribuída diferente.
- Android 16: testar barra de status, navegação por gestos e três botões, teclado `adjustResize`, fontes grandes, rotação/tela ampla e revisão de acessibilidade.
- Testar exportação/restauração de backups v1–v6 e cálculo de saldos, doações, recicláveis, contas pagas e autonomia.

## Referências oficiais (2026-10-31)

- Requisito API 36: https://support.google.com/googleplay/android-developer/answer/11926878?hl=pt-br
- Privacidade e revisão: https://support.google.com/googleplay/android-developer/answer/9859455?hl=en
- Assinatura Play: https://support.google.com/googleplay/android-developer/answer/9842756?hl=pt-br
- Declaração financeira: https://support.google.com/googleplay/android-developer/answer/13849271?hl=pt-br
- Teste fechado: https://support.google.com/googleplay/android-developer/answer/14151465?hl=pt-BR
- Tamanho de página 16 KB: https://developer.android.com/guide/practices/page-sizes
