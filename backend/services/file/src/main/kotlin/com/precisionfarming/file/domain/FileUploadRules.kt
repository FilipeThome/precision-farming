package com.precisionfarming.file.domain

import com.precisionfarming.common.DomainException

data class ValidatedFile(val contentType: String)

object FileUploadRules {
    const val MAX_BYTES = 5L * 1024 * 1024
    const val KIND_MACHINE_PHOTO = "MACHINE_PHOTO"

    fun validate(kind: String, bytes: ByteArray): ValidatedFile {
        if (kind != KIND_MACHINE_PHOTO) {
            throw DomainException("FILE_KIND_UNSUPPORTED", "Unsupported file kind")
        }
        if (bytes.size > MAX_BYTES) {
            throw DomainException("FILE_TOO_LARGE", "File exceeds 5MB")
        }
        val contentType = sniff(bytes)
            ?: throw DomainException("FILE_TYPE_UNSUPPORTED", "Only JPEG, PNG and WebP are allowed")
        return ValidatedFile(contentType)
    }

    fun sniff(bytes: ByteArray): String? {
        if (bytes.size < 12) return null
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }
        if (
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() &&
            bytes[3] == 'G'.code.toByte()
        ) {
            return "image/png"
        }
        val header = String(bytes, 0, 4, Charsets.US_ASCII)
        val fourcc = String(bytes, 8, 4, Charsets.US_ASCII)
        if (header == "RIFF" && fourcc == "WEBP") return "image/webp"
        return null
    }
}
