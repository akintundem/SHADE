package eventplanner.common.qrcode.service;

import eventplanner.common.qrcode.config.QRCodeProperties;
import eventplanner.common.qrcode.generator.QRCodeGenerator;
import eventplanner.common.qrcode.model.QRCodeGenerationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrandedQRCodeServiceTest {

    @Test
    void generateForAttendeeFallsBackWhenRendererFails() {
        QRCodeGenerator generator = mock(QRCodeGenerator.class);
        QRCodeProperties properties = new QRCodeProperties();
        BrandedQRCodeService service = new BrandedQRCodeService(generator, properties);

        when(generator.generate(any(), any())).thenThrow(new RuntimeException("boom"));

        QRCodeGenerationResult result = service.generateForAttendee("sample-data");

        assertThat(result).isNotNull();
        assertThat(result.getPngData()).isNotEmpty();
        verify(generator, times(1)).generate(any(), any());
    }
}
