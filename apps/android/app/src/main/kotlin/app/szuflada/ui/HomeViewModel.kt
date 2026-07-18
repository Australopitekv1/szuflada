package app.szuflada.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.szuflada.core.parsing.ReceiptParser
import app.szuflada.data.ItemRepository
import app.szuflada.data.db.ItemEntity
import app.szuflada.data.db.ItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Pola formularza dodawania — surowe stringi, parsowanie przy zapisie. */
data class AddItemForm(
    val title: String = "",
    val merchant: String = "",
    val amount: String = "",
    val purchaseDate: String = "",
    val warrantyUntil: String = "",
    val ocrText: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ItemRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val items: StateFlow<List<ItemEntity>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) repository.observeAll() else repository.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Auto-fill formularza z wklejonego tekstu paragonu (docelowo: ML Kit OCR). */
    fun fillFromOcr(form: AddItemForm): AddItemForm {
        val parsed = ReceiptParser.parse(form.ocrText)
        return form.copy(
            title = form.title.ifBlank { parsed.merchantGuess ?: "" },
            merchant = form.merchant.ifBlank { parsed.merchantGuess ?: "" },
            amount = form.amount.ifBlank {
                parsed.totalGrosze?.let { "%d,%02d".format(it / 100, it % 100) } ?: ""
            },
            purchaseDate = form.purchaseDate.ifBlank {
                parsed.purchaseDate?.toString() ?: ""
            },
        )
    }

    fun saveItem(form: AddItemForm, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addItem(
                type = ItemType.RECEIPT,
                title = form.title.trim(),
                merchant = form.merchant,
                amountGrosze = ReceiptParser.parseAmountToGrosze(form.amount),
                currency = "PLN",
                purchaseDate = parseDateInput(form.purchaseDate),
                warrantyUntil = parseDateInput(form.warrantyUntil),
                ocrText = form.ocrText,
            )
            onDone()
        }
    }

    private val polishDate = DateTimeFormatter.ofPattern("d.M.uuuu")

    private fun parseDateInput(input: String): LocalDate? {
        val t = input.trim()
        if (t.isEmpty()) return null
        return runCatching { LocalDate.parse(t) }.getOrNull()
            ?: runCatching { LocalDate.parse(t, polishDate) }.getOrNull()
    }
}
