package com.vitalis.feature.tracking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocationAccessTest {
    @Test
    fun `precise wins, approximate asks again, a permanent no goes to settings`() {
        assertThat(locationAccessAfterRequest(fine = true, coarse = true, canAskAgain = false)).isEqualTo(LocationAccess.GRANTED)
        // Approximate is not enough: every sport's accuracy gate rejects a ~1 km fix.
        assertThat(locationAccessAfterRequest(fine = false, coarse = true, canAskAgain = true)).isEqualTo(LocationAccess.APPROXIMATE)
        assertThat(locationAccessAfterRequest(fine = false, coarse = true, canAskAgain = false)).isEqualTo(LocationAccess.BLOCKED)
        assertThat(locationAccessAfterRequest(fine = false, coarse = false, canAskAgain = false)).isEqualTo(LocationAccess.BLOCKED)
        // Denied once: the system will still show its dialog, so stay on the explanation.
        assertThat(locationAccessAfterRequest(fine = false, coarse = false, canAskAgain = true)).isEqualTo(LocationAccess.PRIME)
    }
}
