package org.shannon.koncurrent

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class MyLibraryTest {
    @Test
    fun testMyLanguage() {
        assertThat(MyLibrary().kotlinLanguage().name).isEqualTo("Kotlin")
        assertThat(MyLibrary().kotlinLanguage().hotness).isEqualTo(10)
    }
}