package org.folio.search.service.setter.instance;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.folio.search.domain.dto.Contributor;
import org.folio.search.domain.dto.Instance;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class SortContributorsProcessorTest {
  private final SortContributorsProcessor processor = new SortContributorsProcessor();

  @Test
  void shouldReturnFirstContributorIfNoPrimary() {
    var map = new Instance()
      .addContributorsItem(new Contributor().name("first"))
      .addContributorsItem(new Contributor().name("second"));

    assertThat(processor.getFieldValue(map)).isEqualTo("first");
  }

  @Test
  void shouldReturnPrimaryContributorRegardlessPosition() {
    var map = new Instance()
      .addContributorsItem(new Contributor().name("first"))
      .addContributorsItem(new Contributor().name("second").primary(true));

    assertThat(processor.getFieldValue(map)).isEqualTo("second");
  }

  @Test
  void shouldReturnNullIfEmptyMap() {
    assertNull(processor.getFieldValue(new Instance()));
  }

  @Test
  void shouldReturnNullIfNoContributors() {
    assertNull(processor.getFieldValue(new Instance().title("title")));
  }

  @Test
  void shouldReturnNullIfContributorsIsEmpty() {
    assertNull(processor.getFieldValue(new Instance().contributors(emptyList())));
  }
}
