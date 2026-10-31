package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Documento informativo para a versão 1.0.0. A página pública deve conter o mesmo texto. */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Voltar") } }
        item { Text("Política de privacidade", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Finanças da Viagem · 31 de outubro de 2026", style = MaterialTheme.typography.bodySmall) }
        item { SectionTitle("Dados registrados") }
        item { Text("O aplicativo permite que você registre manualmente receitas, despesas, doações, carteiras, transferências, contas a pagar, coletas e vendas de recicláveis, além de metas e orçamentos. Esses dados ficam armazenados no próprio aparelho.") }
        item { SectionTitle("Internet e compartilhamento") }
        item { Text("O aplicativo funciona offline e não solicita permissão de internet. Não possui login, anúncios, rastreadores ou conexão com contas bancárias. Não envia automaticamente os registros financeiros ao desenvolvedor nem a servidores externos.") }
        item { SectionTitle("Backups e arquivos") }
        item { Text("Ao escolher exportar um backup JSON ou CSV, você seleciona o local do arquivo pelo sistema Android. Os arquivos podem conter dados financeiros pessoais e não são criptografados pelo aplicativo. Você controla o armazenamento e qualquer compartilhamento posterior desses arquivos. Restaurar um backup substitui os dados locais após confirmação.") }
        item { SectionTitle("Armazenamento e exclusão") }
        item { Text("Os registros são mantidos no armazenamento privado do aplicativo até que você os altere, exclua ou remova os dados do aplicativo nas configurações do Android. A desinstalação pode eliminar os dados locais. Backups exportados permanecem no local escolhido por você até serem apagados separadamente.") }
        item { SectionTitle("Permissões") }
        item { Text("O aplicativo não solicita acesso à localização, câmera, microfone, contatos, internet ou contas bancárias. O seletor de arquivos do Android é utilizado somente quando você inicia uma exportação ou importação.") }
        item { SectionTitle("Contato") }
        item { Text("Desenvolvedor: Adriedson Lemos. Informações de contato e atualizações: github.com/adriedsonlemoz. A página pública da política deve ser informada na Google Play Console.") }
    }
}
