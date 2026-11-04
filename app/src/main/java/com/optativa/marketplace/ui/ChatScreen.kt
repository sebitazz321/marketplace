package com.optativa.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.optativa.marketplace.data.Product
import com.optativa.marketplace.logic.AiConfig
import com.optativa.marketplace.logic.MarketBot
import com.optativa.marketplace.logic.MsgSource
import com.optativa.marketplace.logic.Pricing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MarketViewModel, modifier: Modifier = Modifier) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(vm.chat.size, vm.typing) {
        val last = vm.chat.size + (if (vm.typing) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (vm.aiEnabled) "🤖 Mari es un asistente con inteligencia artificial (${AiConfig.PROVIDER}). Puede equivocarse: verifica precios y stock en el catálogo. Nunca compartas contraseñas ni datos de tarjetas."
                else "⚙️ Mari está en modo local sin IA (falta configurar GEMINI_API_KEY).",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("ai_disclosure")
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(vm.chat) { msg -> Bubble(msg, inCart = { vm.cart[it.id] ?: 0 }, onAdd = { vm.addToCart(it) }) }
            if (vm.typing) item { Bubble(ChatMessage("Mari está escribiendo…", MsgSource.LOCAL), { 0 }, {}) }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MarketBot.SUGGESTIONS) { s -> AssistChip(onClick = { vm.send(s) }, label = { Text(s) }) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Pregúntale a Mari…") },
                maxLines = 3,
                supportingText = if (input.length > AiConfig.MAX_INPUT_CHARS - 50) {
                    { Text("${input.length}/${AiConfig.MAX_INPUT_CHARS}") }
                } else null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input")
            )
            Button(
                onClick = { vm.send(input); input = "" },
                enabled = input.isNotBlank() && !vm.typing,
                modifier = Modifier.testTag("chat_send")
            ) { Text("Enviar") }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage, inCart: (Product) -> Int, onAdd: (Product) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (msg.fromUser) 18.dp else 4.dp,
                bottomEnd = if (msg.fromUser) 4.dp else 18.dp
            ),
            color = if (msg.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (msg.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!msg.fromUser) {
                    Text(
                        if (msg.source == MsgSource.AI) "🤖 IA · ${AiConfig.PROVIDER}" else "⚙️ Respuesta local",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    msg.text,
                    modifier = Modifier.semantics {
                        contentDescription = (if (msg.fromUser) "Tú: " else if (msg.source == MsgSource.AI) "Mari, con IA: " else "Mari: ") + msg.text
                    }
                )
                msg.products.forEach { p ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(p.emoji, style = MaterialTheme.typography.headlineSmall)
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Text("${Pricing.format(p.price)} · ${ratingLabel(p.rating)}", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = { onAdd(p) },
                                modifier = Modifier.semantics { contentDescription = "Agregar ${p.name} al carrito" },
                                enabled = inCart(p) < p.stock,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) { Text(if (inCart(p) > 0) "+${inCart(p)}" else "Agregar") }
                        }
                    }
                }
            }
        }
    }
}
