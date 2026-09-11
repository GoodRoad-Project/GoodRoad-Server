package goodroad.validation;

import goodroad.api.ApiErrors.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadValidationServiceTest {
    private final UploadValidationService service = new UploadValidationService();

    @Test
    void acceptsPngBySignatureEvenWhenClientContentTypeIsWrong() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};
        MockMultipartFile file = new MockMultipartFile("file", "image.bin", "application/octet-stream", png);

        UploadValidationService.VerifiedUpload result = service.validate(
                file,
                UploadValidationService.UploadPurpose.REVIEW_PHOTO
        );

        assertEquals("image/png", result.contentType());
        assertEquals(".png", result.extension());
    }

    @Test
    void rejectsExecutableRenamedToJpeg() {
        byte[] executable = new byte[] {'M', 'Z', 1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile("file", "certificate.jpg", "image/jpeg", executable);

        ApiException exception = assertThrows(ApiException.class, () -> service.validate(
                file,
                UploadValidationService.UploadPurpose.VOLUNTEER_CERTIFICATE
        ));

        assertEquals("FILE_CONTENT_TYPE_INVALID", exception.code());
    }

    @Test
    void rejectsPdfAsVolunteerCertificateToAvoidActiveDocumentContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", "%PDF-1.7".getBytes()
        );

        assertThrows(ApiException.class, () -> service.validate(
                file,
                UploadValidationService.UploadPurpose.VOLUNTEER_CERTIFICATE
        ));
    }
}
