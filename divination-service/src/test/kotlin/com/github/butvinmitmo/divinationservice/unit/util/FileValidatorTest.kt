package com.github.butvinmitmo.divinationservice.unit.util

import com.github.butvinmitmo.divinationservice.exception.InvalidFileException
import com.github.butvinmitmo.divinationservice.util.FileValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import kotlin.test.assertTrue

class FileValidatorTest {
    private val maxSizeBytes = 2 * 1024 * 1024L
    private val validator = FileValidator(maxSizeBytes)

    @Test
    fun `validate should pass for valid PNG file`() {
        val filePart = createMockFilePart("image.png", MediaType.IMAGE_PNG)

        assertDoesNotThrow {
            validator.validate(filePart, 1024)
        }
    }

    @Test
    fun `validate should pass for valid JPG file`() {
        val filePart = createMockFilePart("image.jpg", MediaType.IMAGE_JPEG)

        assertDoesNotThrow {
            validator.validate(filePart, 1024)
        }
    }

    @Test
    fun `validate should pass for valid JPEG file`() {
        val filePart = createMockFilePart("image.jpeg", MediaType.IMAGE_JPEG)

        assertDoesNotThrow {
            validator.validate(filePart, 1024)
        }
    }

    @Test
    fun `validate should fail for invalid extension`() {
        val filePart = createMockFilePart("document.pdf", MediaType.APPLICATION_PDF)

        val exception =
            assertThrows<InvalidFileException> {
                validator.validate(filePart, 1024)
            }
        assertTrue(exception.message!!.contains("Invalid file extension"))
    }

    @Test
    fun `validate should fail for file exceeding max size`() {
        val filePart = createMockFilePart("image.png", MediaType.IMAGE_PNG)
        val oversizedBytes = maxSizeBytes + 1

        val exception =
            assertThrows<InvalidFileException> {
                validator.validate(filePart, oversizedBytes)
            }
        assertTrue(exception.message!!.contains("File size exceeds"))
    }

    @Test
    fun `validate should fail for invalid content type`() {
        val headers = mock<HttpHeaders>()
        whenever(headers.contentType).thenReturn(MediaType.TEXT_PLAIN)

        val filePart = mock<FilePart>()
        whenever(filePart.filename()).thenReturn("image.png")
        whenever(filePart.headers()).thenReturn(headers)

        val exception =
            assertThrows<InvalidFileException> {
                validator.validate(filePart, 1024)
            }
        assertTrue(exception.message!!.contains("Invalid content type"))
    }

    @Test
    fun `validate should fail when content type is null`() {
        val headers = mock<HttpHeaders>()
        whenever(headers.contentType).thenReturn(null)

        val filePart = mock<FilePart>()
        whenever(filePart.filename()).thenReturn("image.png")
        whenever(filePart.headers()).thenReturn(headers)

        val exception =
            assertThrows<InvalidFileException> {
                validator.validate(filePart, 1024)
            }
        assertTrue(exception.message!!.contains("Invalid content type"))
    }

    private fun createMockFilePart(
        filename: String,
        contentType: MediaType,
    ): FilePart {
        val headers = mock<HttpHeaders>()
        whenever(headers.contentType).thenReturn(contentType)

        val filePart = mock<FilePart>()
        whenever(filePart.filename()).thenReturn(filename)
        whenever(filePart.headers()).thenReturn(headers)

        return filePart
    }
}
