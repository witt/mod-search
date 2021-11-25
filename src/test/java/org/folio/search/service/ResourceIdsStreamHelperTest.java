package org.folio.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.search.utils.TestConstants.RESOURCE_NAME;
import static org.folio.search.utils.TestConstants.TENANT_ID;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.folio.search.exception.SearchServiceException;
import org.folio.search.model.service.CqlResourceIdsRequest;
import org.folio.search.utils.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ResourceIdsStreamHelperTest {

  @InjectMocks private ResourceIdsStreamHelper resourceIdsStreamHelper;
  @Mock private ResourceIdService resourceIdService;

  @Test
  void streamResourceIds_positive() throws IOException {
    var servletRequestAttributes = mock(ServletRequestAttributes.class);
    RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    var httpServletResponse = mock(HttpServletResponse.class);
    var outputStream = mock(ServletOutputStream.class);
    when(servletRequestAttributes.getResponse()).thenReturn(httpServletResponse);
    when(httpServletResponse.getOutputStream()).thenReturn(outputStream);

    var request = CqlResourceIdsRequest.of("id=*", RESOURCE_NAME, TENANT_ID, "id");
    doNothing().when(resourceIdService).streamResourceIds(request, outputStream);

    var actual = resourceIdsStreamHelper.streamResourceIds(request);
    assertThat(actual).isEqualTo(ResponseEntity.ok().build());
  }

  @Test
  void streamResourceIds_negative_nullRequestAttributes() {
    RequestContextHolder.setRequestAttributes(null);

    var request = CqlResourceIdsRequest.of("id=*", RESOURCE_NAME, TENANT_ID, "id");
    assertThatThrownBy(() -> resourceIdsStreamHelper.streamResourceIds(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Request attributes must be not null");
  }

  @Test
  void streamResourceIds_negative_nullHttpServletResponse() {
    var servletRequestAttributes = mock(ServletRequestAttributes.class);
    RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    when(servletRequestAttributes.getResponse()).thenReturn(null);

    var request = CqlResourceIdsRequest.of("id=*", RESOURCE_NAME, TENANT_ID, "id");
    assertThatThrownBy(() -> resourceIdsStreamHelper.streamResourceIds(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("HttpServletResponse must be not null");
  }

  @Test
  void streamResourceIds_negative_errorOnReceivingOutputStream() throws IOException {
    var servletRequestAttributes = mock(ServletRequestAttributes.class);
    RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    var httpServletResponse = mock(HttpServletResponse.class);
    when(servletRequestAttributes.getResponse()).thenReturn(httpServletResponse);
    when(httpServletResponse.getOutputStream()).thenThrow(new IOException("error"));

    var request = CqlResourceIdsRequest.of("id=*", RESOURCE_NAME, TENANT_ID, "id");
    assertThatThrownBy(() -> resourceIdsStreamHelper.streamResourceIds(request))
      .isInstanceOf(SearchServiceException.class)
      .hasMessage("Failed to get output stream from response");
  }
}