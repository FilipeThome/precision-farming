package com.precisionfarming.file

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import com.precisionfarming.file.domain.FileUploadRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FileDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("file-001"), DemoIds.uuid("file-001"))
    }

    @Test
    fun acceptsJpegPngWebpUnderLimit() {
        assertEquals("image/jpeg", FileUploadRules.validate("MACHINE_PHOTO", jpeg()).contentType)
        assertEquals("image/png", FileUploadRules.validate("MACHINE_PHOTO", png()).contentType)
        assertEquals("image/webp", FileUploadRules.validate("MACHINE_PHOTO", webp()).contentType)
    }

    @Test
    fun rejectsUnsupportedKind() {
        val ex = assertThrows(DomainException::class.java) {
            FileUploadRules.validate("NDVI_DEMO", jpeg())
        }
        assertEquals("FILE_KIND_UNSUPPORTED", ex.code)
    }

    @Test
    fun rejectsOversizedFile() {
        val bytes = ByteArray(FileUploadRules.MAX_BYTES.toInt() + 1)
        bytes[0] = 0xFF.toByte(); bytes[1] = 0xD8.toByte(); bytes[2] = 0xFF.toByte()
        val ex = assertThrows(DomainException::class.java) {
            FileUploadRules.validate("MACHINE_PHOTO", bytes)
        }
        assertEquals("FILE_TOO_LARGE", ex.code)
    }

    @Test
    fun rejectsWrongType() {
        val ex = assertThrows(DomainException::class.java) {
            FileUploadRules.validate("MACHINE_PHOTO", ByteArray(16) { 'A'.code.toByte() })
        }
        assertEquals("FILE_TYPE_UNSUPPORTED", ex.code)
    }

    private fun jpeg() = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + ByteArray(13)
    private fun png() = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()) + ByteArray(12)
    private fun webp(): ByteArray {
        val bytes = ByteArray(16)
        "RIFF".toByteArray().copyInto(bytes, 0)
        "WEBP".toByteArray().copyInto(bytes, 8)
        return bytes
    }
}
