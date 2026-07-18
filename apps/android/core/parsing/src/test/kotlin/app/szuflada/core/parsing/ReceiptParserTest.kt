package app.szuflada.core.parsing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReceiptParserTest {

    private val biedronkaLike = """
        BIEDRONKA "CODZIENNIE NISKIE CENY" 4521
        JERONIMO MARTINS POLSKA S.A.
        ul. Żniwna 5, 62-025 Kostrzyn
        NIP 779-10-11-327
        2026-07-15 14:23                 PARAGON FISKALNY
        Mleko UHT 3,2% 1L          x2    7,98 A
        Chleb żytni                x1    5,49 A
        Masło ekstra 200g          x1   38,52 A
        SPRZEDAŻ OPODATKOWANA A         51,99
        PTU A 5,00%                      2,48
        SUMA PLN                        51,99
        NR SYS. 4521/2026
    """.trimIndent()

    private val lidlLike = """
        LIDL sp. z o.o. sp. k.
        ul. Poznańska 48, Jankowice
        NIP: 7811897358
        Data: 12.03.2026
        Woda mineralna 1,5L              2,39 A
        RAZEM                           89,45 PLN
    """.trimIndent()

    @Test
    fun `parsuje paragon w stylu Biedronki`() {
        val r = ReceiptParser.parse(biedronkaLike)
        assertEquals(5199L, r.totalGrosze, "SUMA PLN ma priorytet nad kwotami pozycji")
        assertEquals("PLN", r.currency)
        assertEquals(LocalDate.of(2026, 7, 15), r.purchaseDate)
        assertEquals("7791011327", r.nip)
        assertTrue(r.merchantGuess!!.startsWith("BIEDRONKA"))
    }

    @Test
    fun `parsuje paragon w stylu Lidla`() {
        val r = ReceiptParser.parse(lidlLike)
        assertEquals(8945L, r.totalGrosze)
        assertEquals("PLN", r.currency)
        assertEquals(LocalDate.of(2026, 3, 12), r.purchaseDate)
        assertEquals("7811897358", r.nip)
        assertTrue(r.merchantGuess!!.startsWith("LIDL"))
    }

    // ── Kwoty ─────────────────────────────────────────────────────────────

    @Test
    fun `DO ZAPLATY wygrywa z RAZEM`() {
        val r = ReceiptParser.parse(
            """
            RAZEM                999,99
            DO ZAPŁATY           123,45
            """.trimIndent(),
        )
        assertEquals(12345L, r.totalGrosze)
    }

    @Test
    fun `kwota z separatorem tysiecy`() {
        val r = ReceiptParser.parse("SUMA PLN 2 499,00")
        assertEquals(249900L, r.totalGrosze)
    }

    @Test
    fun `fallback do najwiekszej kwoty gdy brak slow kluczowych`() {
        val r = ReceiptParser.parse(
            """
            Pozycja A 12,50
            Pozycja B 49,99
            Pozycja C 7,20
            """.trimIndent(),
        )
        assertEquals(4999L, r.totalGrosze)
    }

    @Test
    fun `brak kwot daje null`() {
        assertNull(ReceiptParser.parse("żadnych liczb tutaj").totalGrosze)
    }

    @Test
    fun `parseAmountToGrosze - pole formularza`() {
        assertEquals(4999L, ReceiptParser.parseAmountToGrosze("49,99"))
        assertEquals(4999L, ReceiptParser.parseAmountToGrosze("49.99"))
        assertEquals(249900L, ReceiptParser.parseAmountToGrosze("2 499,00"))
        assertEquals(1200L, ReceiptParser.parseAmountToGrosze("12"))
        assertEquals(249900L, ReceiptParser.parseAmountToGrosze("2 499"))
        assertNull(ReceiptParser.parseAmountToGrosze(""))
        assertNull(ReceiptParser.parseAmountToGrosze("abc"))
        assertNull(ReceiptParser.parseAmountToGrosze("12,3"))
    }

    // ── Daty ──────────────────────────────────────────────────────────────

    @Test
    fun `format ISO yyyy-MM-dd`() {
        assertEquals(
            LocalDate.of(2026, 1, 31),
            ReceiptParser.parse("data 2026-01-31 koniec").purchaseDate,
        )
    }

    @Test
    fun `format polski dd_MM_yyyy z kropkami i ukosnikami`() {
        assertEquals(
            LocalDate.of(2025, 12, 24),
            ReceiptParser.parse("24.12.2025").purchaseDate,
        )
        assertEquals(
            LocalDate.of(2025, 2, 1),
            ReceiptParser.parse("1/2/2025").purchaseDate,
        )
    }

    @Test
    fun `nieistniejaca data jest odrzucana`() {
        assertNull(ReceiptParser.parse("32.13.2026").purchaseDate)
    }

    // ── NIP ───────────────────────────────────────────────────────────────

    @Test
    fun `NIP z myslnikami w obu formatach grupowania`() {
        assertEquals("7791011327", ReceiptParser.parse("NIP 779-10-11-327").nip)
        assertEquals("5260250274", ReceiptParser.parse("NIP: 526-025-02-74").nip)
    }

    @Test
    fun `NIP bez etykiety tez znajdowany po sumie kontrolnej`() {
        assertEquals("7811897358", ReceiptParser.parse("Firma 7811897358 Sp. z o.o.").nip)
    }

    @Test
    fun `zla suma kontrolna odrzuca kandydata`() {
        assertNull(ReceiptParser.parse("NIP 779-10-11-328").nip)
    }

    @Test
    fun `suma kontrolna - przypadki brzegowe`() {
        assertTrue(ReceiptParser.hasValidNipChecksum("7791011327"))
        assertFalse(ReceiptParser.hasValidNipChecksum("7791011328"))
        assertFalse(ReceiptParser.hasValidNipChecksum("123"))
        assertFalse(ReceiptParser.hasValidNipChecksum("abcdefghij"))
    }

    // ── Sprzedawca ────────────────────────────────────────────────────────

    @Test
    fun `pomija linie PARAGON FISKALNY przy zgadywaniu sklepu`() {
        val r = ReceiptParser.parse(
            """
            PARAGON FISKALNY
            Żabka Polska
            """.trimIndent(),
        )
        assertEquals("Żabka Polska", r.merchantGuess)
    }

    @Test
    fun `pusty tekst daje pusty wynik`() {
        assertEquals(ParsedReceipt(), ReceiptParser.parse(""))
    }
}
