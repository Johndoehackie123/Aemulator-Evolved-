package app.aemu.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM checks for the per-layout field preference rules. */
class HubFieldsTest {

    @Test
    fun blankPreferencesUseCanonicalOrder() {
        assertEquals(HubFieldDefaults.ORDER, HubFieldDefaults.decode(null))
        assertEquals(HubFieldDefaults.ORDER, HubFieldDefaults.decode(""))
    }

    @Test
    fun decodingKeepsSavedOrderAndRestoresMissingFields() {
        assertEquals(
            listOf(HubField.CHECKSUM, HubField.SIZE, HubField.STATUS, HubField.SOURCE, HubField.FORMAT, HubField.REPORT),
            HubFieldDefaults.decode("CHECKSUM,SIZE,STATUS,BOGUS"),
        )
    }

    @Test
    fun roundTripEncodeDecodeIsStable() {
        val fields = listOf(HubField.REPORT, HubField.STATUS)
        assertEquals(fields, HubFieldDefaults.decode(HubFieldDefaults.encode(fields)).take(2))
    }

    @Test
    fun togglingRestoresCanonicalPosition() {
        val withoutSize = HubFieldDefaults.toggle(HubFieldDefaults.ORDER, HubField.SIZE)
        assertFalse(HubField.SIZE in withoutSize)
        assertEquals(HubFieldDefaults.ORDER, HubFieldDefaults.toggle(withoutSize, HubField.SIZE))
    }

    @Test
    fun movingFieldsRespectsBoundaries() {
        val first = HubFieldDefaults.move(HubFieldDefaults.ORDER, HubField.STATUS, -1)
        assertEquals(HubFieldDefaults.ORDER, first)
        val moved = HubFieldDefaults.move(HubFieldDefaults.ORDER, HubField.STATUS, 1)
        assertEquals(HubField.SIZE, moved.first())
    }
}
