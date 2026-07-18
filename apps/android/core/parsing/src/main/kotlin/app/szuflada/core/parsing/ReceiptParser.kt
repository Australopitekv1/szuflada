package app.szuflada.core.parsing

import java.time.LocalDate

/**
 * Wynik auto-parsowania tekstu OCR polskiego paragonu (Faza 1 briefu).
 * Wszystkie pola opcjonalne — parser jest heurystyką, użytkownik zawsze
 * może poprawić dane ręcznie.
 */
data class ParsedReceipt(
    /** Kwota "do zapłaty" w groszach (unika błędów zaokrągleń Double). */
    val totalGrosze: Long? = null,
    val currency: String? = null,
    val purchaseDate: LocalDate? = null,
    /** NIP sprzedawcy — 10 cyfr, zweryfikowana suma kontrolna. */
    val nip: String? = null,
    /** Pierwsza sensowna linia paragonu — zwykle nazwa sklepu. */
    val merchantGuess: String? = null,
)

/**
 * Parser tekstu OCR z ML Kit dla polskich paragonów fiskalnych.
 * Czysta funkcja bez zależności — testowalna na JVM.
 */
object ReceiptParser {

    fun parse(ocrText: String): ParsedReceipt {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return ParsedReceipt(
            totalGrosze = findTotalGrosze(lines),
            currency = findCurrency(ocrText),
            purchaseDate = findDate(ocrText),
            nip = findNip(ocrText),
            merchantGuess = guessMerchant(lines),
        )
    }

    // ── Kwota ─────────────────────────────────────────────────────────────

    /** Kwota w formacie polskim: 2 499,00 / 2499,00 / 49.99. */
    private val amountRegex = Regex("""(\d{1,3}(?:[ .]\d{3})*|\d+)[,.](\d{2})(?!\d)""")

    /**
     * Słowa kluczowe linii z kwotą całkowitą, w kolejności priorytetu.
     * "SUMA PLN" na paragonie fiskalnym to kwota transakcji.
     */
    private val totalKeywords = listOf(
        Regex("""SUMA\s*:?\s*PLN""", RegexOption.IGNORE_CASE),
        Regex("""DO\s+ZAP[ŁL]ATY""", RegexOption.IGNORE_CASE),
        Regex("""SUMA""", RegexOption.IGNORE_CASE),
        Regex("""RAZEM""", RegexOption.IGNORE_CASE),
        Regex("""[ŁL][ĄA]CZNIE""", RegexOption.IGNORE_CASE),
    )

    private fun findTotalGrosze(lines: List<String>): Long? {
        for (keyword in totalKeywords) {
            for (line in lines) {
                if (!keyword.containsMatchIn(line)) continue
                val amount = amountRegex.findAll(line).lastOrNull() ?: continue
                return toGrosze(amount)
            }
        }
        // Fallback: największa kwota w tekście (paragony z uciętym nagłówkiem).
        return lines
            .flatMap { amountRegex.findAll(it) }
            .map { toGrosze(it) }
            .maxOrNull()
    }

    private fun toGrosze(match: MatchResult): Long {
        val zlote = match.groupValues[1].replace(" ", "").replace(".", "")
        val grosze = match.groupValues[2]
        return zlote.toLong() * 100 + grosze.toLong()
    }

    private fun findCurrency(text: String): String? = when {
        Regex("""\bPLN\b|\bz[łl]\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "PLN"
        Regex("""\bEUR\b|€""").containsMatchIn(text) -> "EUR"
        else -> null
    }

    // ── Data ──────────────────────────────────────────────────────────────

    private val dmyRegex = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](\d{4})\b""")
    private val ymdRegex = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")

    private fun findDate(text: String): LocalDate? {
        ymdRegex.findAll(text).forEach { m ->
            toDate(m.groupValues[1], m.groupValues[2], m.groupValues[3])?.let { return it }
        }
        dmyRegex.findAll(text).forEach { m ->
            toDate(m.groupValues[3], m.groupValues[2], m.groupValues[1])?.let { return it }
        }
        return null
    }

    private fun toDate(y: String, m: String, d: String): LocalDate? = try {
        LocalDate.of(y.toInt(), m.toInt(), d.toInt())
    } catch (_: Exception) {
        null
    }

    // ── NIP ───────────────────────────────────────────────────────────────

    /** 10 cyfr z opcjonalnymi separatorami; formaty XXX-XXX-XX-XX i XXX-XX-XX-XXX. */
    private val nipCandidateRegex =
        Regex("""\b(\d{3})[- ]?(\d{2,3})[- ]?(\d{2})[- ]?(\d{2,3})\b""")

    private val nipLabelRegex = Regex("""NIP\s*[:.]?\s*""", RegexOption.IGNORE_CASE)

    private fun findNip(text: String): String? {
        // Najpierw kandydaci z etykietą "NIP", potem reszta tekstu.
        val labelled = nipLabelRegex.findAll(text).mapNotNull { label ->
            nipCandidateRegex.find(text, label.range.last)?.let { m ->
                if (m.range.first - label.range.last <= 2) m else null
            }
        }
        val all = labelled + nipCandidateRegex.findAll(text)
        return all
            .map { it.groupValues.drop(1).joinToString("") }
            .firstOrNull { it.length == 10 && hasValidNipChecksum(it) }
    }

    /** Suma kontrolna NIP: wagi 6,5,7,2,3,4,5,6,7; mod 11 musi dać 10. cyfrę. */
    internal fun hasValidNipChecksum(digits: String): Boolean {
        if (digits.length != 10 || digits.any { !it.isDigit() }) return false
        val weights = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)
        val sum = weights.indices.sumOf { weights[it] * (digits[it] - '0') }
        val control = sum % 11
        return control != 10 && control == digits[9] - '0'
    }

    // ── Sprzedawca ────────────────────────────────────────────────────────

    private val nonMerchantLine =
        Regex("""PARAGON|FISKALNY|NIEFISKALNY|www\.|^\d""", RegexOption.IGNORE_CASE)

    private fun guessMerchant(lines: List<String>): String? =
        lines.take(4).firstOrNull { it.length in 3..64 && !nonMerchantLine.containsMatchIn(it) }
}
