package org.folio.search.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.search.sample.SampleAuthorities.getAuthoritySampleAsMap;
import static org.folio.search.sample.SampleAuthorities.getAuthoritySampleId;
import static org.folio.search.utils.TestUtils.parseResponse;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import java.util.stream.Stream;
import org.folio.search.domain.dto.Authority;
import org.folio.search.domain.dto.AuthoritySearchResult;
import org.folio.search.support.base.BaseIntegrationTest;
import org.folio.search.utils.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

@IntegrationTest
class SearchAuthorityIT extends BaseIntegrationTest {

  private static final String AUTHORIZED_TYPE = "Authorized";
  private static final String REFERENCE_TYPE = "Reference";
  private static final String AUTH_REF_TYPE = "Auth/Ref";
  private static final String OTHER_HEADING_TYPE = "Other";

  @BeforeAll
  static void prepare() {
    setUpTenant(Authority.class, 21, getAuthoritySampleAsMap());
  }

  @AfterAll
  static void cleanUp() {
    removeTenant();
  }

  @MethodSource("testDataProvider")
  @DisplayName("search by authorities (single authority found)")
  @ParameterizedTest(name = "[{index}] query={0}, value=''{1}''")
  void searchByAuthorities_parameterized(String query, String value) throws Exception {
    doSearchByAuthorities(prepareQuery(query, value))
      .andExpect(jsonPath("$.totalRecords", is(1)))
      .andExpect(jsonPath("$.authorities[0].id", is(getAuthoritySampleId())));
  }

  @CsvSource({
    "cql.allRecords=1,",
    "id={value}, \"\"",
    "id=={value}, 55294032-fcf6-45cc-b6da-4420a61ef72c",
    "id=={value}, 55294032-fcf6-45cc-b6da-*"
  })
  @ParameterizedTest(name = "[{index}] query={0}, value=''{1}''")
  @DisplayName("search by authorities (check that they are divided correctly)")
  void searchByAuthorities_parameterized_all(String query, String value) throws Exception {
    var response = doSearchByAuthorities(prepareQuery(query, value)).andExpect(jsonPath("$.totalRecords", is(21)));
    var actual = parseResponse(response, AuthoritySearchResult.class);
    assertThat(actual.getAuthorities()).isEqualTo(List.of(
      authority("Personal Name", AUTHORIZED_TYPE, "Gary A. Wills"),
      authority("Personal Name", REFERENCE_TYPE, "a stf personal name"),
      authority(OTHER_HEADING_TYPE, AUTH_REF_TYPE, "a saft personal name"),

      authority("Corporate Name", AUTHORIZED_TYPE, "a corporate name"),
      authority("Corporate Name", REFERENCE_TYPE, "a stf corporate name"),
      authority(OTHER_HEADING_TYPE, AUTH_REF_TYPE, "a saft corporate name"),

      authority("Conference Name", AUTHORIZED_TYPE, "a meeting name"),
      authority("Conference Name", REFERENCE_TYPE, "a sft meeting name"),
      authority(OTHER_HEADING_TYPE, AUTH_REF_TYPE, "a saft meeting name"),

      authority("Geographic Name", AUTHORIZED_TYPE, "a geographic name"),
      authority("Geographic Name", REFERENCE_TYPE, "a sft geographic name"),
      authority(OTHER_HEADING_TYPE, AUTH_REF_TYPE, "a saft geographic name"),

      authority("Uniform Title", AUTHORIZED_TYPE, "an uniform title"),
      authority("Uniform Title", REFERENCE_TYPE, "a sft uniform title"),
      authority(OTHER_HEADING_TYPE, AUTH_REF_TYPE, "a saft uniform title"),

      authority("Topical", AUTHORIZED_TYPE, "a topical term"),
      authority("Topical", REFERENCE_TYPE, "a sft topical term"),
      authority(OTHER_HEADING_TYPE, AUTH_REF_TYPE, "a saft topical term"),

      authority("Genre", AUTHORIZED_TYPE, "a genre term"),
      authority("Genre", REFERENCE_TYPE, "a sft genre term"),
      authority(OTHER_HEADING_TYPE, AUTH_REF_TYPE, "a saft genre term")
    ));
  }

  private static Stream<Arguments> testDataProvider() {
    return Stream.of(
      arguments("personalName all {value}", "\"Gary A. Wills\""),
      arguments("personalName all {value}", "gary"),
      arguments("personalName == {value}", "\"gary a.*\""),
      arguments("personalName == {value} and headingType==\"Personal Name\"", "gary"),
      arguments("personalName == {value} and authRefType==\"Authorized\"", "gary")
    );
  }

  private static Authority authority(String headingType, String authRefType, String headingRef) {
    return new Authority()
      .id(getAuthoritySampleId())
      .headingType(headingType)
      .authRefType(authRefType)
      .headingRef(headingRef);
  }
}