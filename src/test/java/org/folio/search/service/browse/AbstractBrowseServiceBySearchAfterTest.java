package org.folio.search.service.browse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.folio.search.model.BrowseResult;
import org.folio.search.model.SearchResult;
import org.folio.search.model.service.BrowseContext;
import org.folio.search.model.service.BrowseRequest;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.opensearch.search.builder.SearchSourceBuilder;

@UnitTest
class AbstractBrowseServiceBySearchAfterTest {

  @Test
  void initialize_negative_instanceWithoutGenerics() {
    assertThatThrownBy(TestBrowseService::new)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Failed to resolve generic types for TestBrowseService.");
  }

  @SuppressWarnings("rawtypes")
  private static final class TestBrowseService extends AbstractBrowseServiceBySearchAfter {

    @Override
    protected SearchSourceBuilder getAnchorSearchQuery(BrowseRequest r, BrowseContext c) {
      return null;
    }

    @Override
    protected SearchSourceBuilder getSearchQuery(BrowseRequest r, BrowseContext c, boolean isBrowsingForward) {
      return null;
    }

    @Override
    protected Object getEmptyBrowseItem(BrowseContext context) {
      return null;
    }

    @Override
    protected BrowseResult mapToBrowseResult(BrowseContext context, SearchResult searchResult, boolean isAnchor) {
      return null;
    }

    @Override
    protected String getValueForBrowsing(Object browseItem) {
      return null;
    }
  }
}
