package org.folio.search.controller;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.search.domain.dto.TenantConfiguredFeature.BROWSE_CN_INTERMEDIATE_VALUES;
import static org.folio.search.support.base.ApiEndpoints.instanceCallNumberBrowsePath;
import static org.folio.search.utils.TestConstants.TENANT_ID;
import static org.folio.search.utils.TestUtils.cnBrowseItem;
import static org.folio.search.utils.TestUtils.getShelfKeyFromCallNumber;
import static org.folio.search.utils.TestUtils.parseResponse;
import static org.folio.search.utils.TestUtils.randomId;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.folio.search.domain.dto.CallNumberBrowseResult;
import org.folio.search.domain.dto.Instance;
import org.folio.search.domain.dto.Item;
import org.folio.search.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.search.support.base.BaseIntegrationTest;
import org.folio.spring.test.type.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@IntegrationTest
class BrowseCallNumberIT extends BaseIntegrationTest {

  private static final Instance[] INSTANCES = instances();
  private static final Map<String, Instance> INSTANCE_MAP =
    Arrays.stream(INSTANCES).collect(toMap(Instance::getTitle, identity()));

  @BeforeAll
  static void prepare() {
    setUpTenant(INSTANCES);
  }

  @AfterAll
  static void cleanUp() {
    removeTenant();
  }

  @MethodSource("callNumberBrowsingDataProvider")
  @DisplayName("browseByCallNumber_parameterized")
  @ParameterizedTest(name = "[{index}] query={0}, value=''{1}'', limit={2}")
  void browseByCallNumber_parameterized(String query, String anchor, Integer limit, CallNumberBrowseResult expected) {
    var request = get(instanceCallNumberBrowsePath())
      .param("expandAll", "true")
      .param("query", prepareQuery(query, '"' + anchor + '"'))
      .param("limit", String.valueOf(limit));
    var actual = parseResponse(doGet(request), CallNumberBrowseResult.class);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void browseByCallNumber_browsingAroundWhenPrecedingRecordsCountIsSpecified() {
    var request = get(instanceCallNumberBrowsePath())
      .param("query", prepareQuery("callNumber < {value} or callNumber >= {value}", "\"CE 16 B6713 X 41993\""))
      .param("limit", "5")
      .param("expandAll", "true")
      .param("precedingRecordsCount", "4");
    var actual = parseResponse(doGet(request), CallNumberBrowseResult.class);
    assertThat(actual).isEqualTo(new CallNumberBrowseResult()
      .totalRecords(36).prev(null).next("CE 16 B6713 X 41993").items(List.of(
        cnBrowseItem(instance("instance #31"), "AB 14 C72 NO 220"),
        cnBrowseItem(instance("instance #25"), "AC 11 A4 VOL 235"),
        cnBrowseItem(instance("instance #08"), "AC 11 A67 X 42000"),
        cnBrowseItem(instance("instance #18"), "AC 11 E8 NO 14 P S1487"),
        cnBrowseItem(instance("instance #44"), "CE 16 B6713 X 41993", true)
      )));
  }

  //https://issues.folio.org/browse/MSEARCH-513
  @Test
  void browseByCallNumber_browsingVeryFirstCallNumberWithNoException() {
    var request = get(instanceCallNumberBrowsePath())
        .param("query", prepareQuery("callNumber >= {value} or callNumber < {value}", "\"AB 14 C72 NO 220\""))
        .param("limit", "10")
        .param("highlightMatch", "true")
        .param("expandAll", "true")
        .param("precedingRecordsCount", "5");
    var actual = parseResponse(doGet(request), CallNumberBrowseResult.class);
    assertThat(actual).isEqualTo(new CallNumberBrowseResult()
        .totalRecords(32).prev(null).next("CE 16 B6713 X 41993").items(List.of(
            cnBrowseItem(instance("instance #31"), "AB 14 C72 NO 220", true),
            cnBrowseItem(instance("instance #25"), "AC 11 A4 VOL 235"),
            cnBrowseItem(instance("instance #08"), "AC 11 A67 X 42000"),
            cnBrowseItem(instance("instance #18"), "AC 11 E8 NO 14 P S1487"),
            cnBrowseItem(instance("instance #44"), "CE 16 B6713 X 41993")
        )));
  }

  @Test
  void browseByCallNumber_browsingAroundWhenMultipleAnchors() {
    var request = get(instanceCallNumberBrowsePath())
      .param("query", prepareQuery("callNumber < {value} or callNumber >= {value}", "\"J29.29:M54/990\""))
      .param("limit", "5")
      .param("expandAll", "true")
      .param("precedingRecordsCount", "4");
    var actual = parseResponse(doGet(request), CallNumberBrowseResult.class);
    var expected = new CallNumberBrowseResult()
      .totalRecords(41).prev("GA 16 D64 41548A").next("J29.29:M54/990").items(List.of(
        cnBrowseItem(instance("instance #39"), "GA 16 D64 41548A"),
        cnBrowseItem(instance("instance #30"), "GA 16 G32 41557 V1"),
        cnBrowseItem(instance("instance #30"), "GA 16 G32 41557 V2"),
        cnBrowseItem(instance("instance #30"), "GA 16 G32 41557 V3"),
        cnBrowseItem(instance("instance #47"), "J29.29:M54/990", true)
      ));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void browseByCallNumber_browsingAroundWithoutHighlightMatch() {
    var request = get(instanceCallNumberBrowsePath())
      .param("query", prepareQuery("callNumber < {value} or callNumber >= {value}", "\"CE 16 B6713 X 41993\""))
      .param("limit", "5")
      .param("expandAll", "true")
      .param("highlightMatch", "false");
    var actual = parseResponse(doGet(request), CallNumberBrowseResult.class);

    assertThat(actual).isEqualTo(new CallNumberBrowseResult()
      .totalRecords(36).prev("AC 11 A67 X 42000").next("CE 16 D86 X 41998").items(List.of(
        cnBrowseItem(instance("instance #08"), "AC 11 A67 X 42000"),
        cnBrowseItem(instance("instance #18"), "AC 11 E8 NO 14 P S1487"),
        cnBrowseItem(instance("instance #44"), "CE 16 B6713 X 41993"),
        cnBrowseItem(instance("instance #45"), "CE 16 B6724 41993"),
        cnBrowseItem(instance("instance #04"), "CE 16 D86 X 41998")
      )));
  }

  @Test
  void browseByCalNumber_browseAroundWithEnabledIntermediateValues() throws InterruptedException {
    enableFeature(BROWSE_CN_INTERMEDIATE_VALUES);

    var request = get(instanceCallNumberBrowsePath())
      .param("query", prepareQuery("callNumber < {value} or callNumber >= {value}", "\"DA 3880 O5 C3 V1\""))
      .param("limit", "7")
      .param("expandAll", "true");
    var actual = parseResponse(doGet(request), CallNumberBrowseResult.class);

    assertThat(actual).isEqualTo(new CallNumberBrowseResult()
      .totalRecords(49).prev("DA 3870 B55 41868").next("DA 3880 O6 D5").items(List.of(
        cnBrowseItem(instance("instance #41"), "DA 3870 B55 41868"),
        cnBrowseItem(instance("instance #07"), "DA 3870 H47 41975"),
        cnBrowseItem(instance("instance #11"), "DA 3880 K56 M27 41984"),
        cnBrowseItem(instance("instance #32"), "DA 3880 O5 C3 V1", true),
        cnBrowseItem(instance("instance #32"), "DA 3880 O5 C3 V2"),
        cnBrowseItem(instance("instance #32"), "DA 3880 O5 C3 V3"),
        cnBrowseItem(instance("instance #29"), "DA 3880 O6 D5")
      )));

    disableFeature(BROWSE_CN_INTERMEDIATE_VALUES);
  }

  private static Stream<Arguments> callNumberBrowsingDataProvider() {
    var aroundQuery = "callNumber > {value} or callNumber < {value}";
    var aroundIncludingQuery = "callNumber >= {value} or callNumber < {value}";
    var forwardQuery = "itemEffectiveShelvingOrder > {value}";
    var forwardIncludingQuery = "itemEffectiveShelvingOrder >= {value}";
    var backwardQuery = "itemEffectiveShelvingOrder < {value}";
    var backwardIncludingQuery = "itemEffectiveShelvingOrder <= {value}";

    var firstAnchorCallNumber = "CE 210 K297 41858";
    var secondAnchorCallNumber = "DA 3890 A1";
    var firstAnchorShelfKey = getShelfKeyFromCallNumber(firstAnchorCallNumber);
    var secondAnchorShelfKey = getShelfKeyFromCallNumber(secondAnchorCallNumber);

    return Stream.of(
      arguments(aroundQuery, firstAnchorCallNumber, 5, new CallNumberBrowseResult()
        .totalRecords(36).prev("CE 16 B6724 41993").next("DA 3700 C95 NO 18").items(List.of(
          cnBrowseItem(instance("instance #45"), "CE 16 B6724 41993"),
          cnBrowseItem(instance("instance #04"), "CE 16 D86 X 41998"),
          cnBrowseItem(0, "CE 210 K297 41858", true),
          cnBrowseItem(instance("instance #36"), "DA 3700 B91 L79"),
          cnBrowseItem(instance("instance #09"), "DA 3700 C95 NO 18")
        ))),

      arguments(aroundQuery, secondAnchorCallNumber, 5, new CallNumberBrowseResult()
        .totalRecords(49).prev("DA 3880 O6 M81").next("DA 3890 A2 B76 42002").items(List.of(
          cnBrowseItem(instance("instance #13"), "DA 3880 O6 M81"),
          cnBrowseItem(instance("instance #02"), "DA 3880 O6 M96"),
          cnBrowseItem(0, "DA 3890 A1", true),
          cnBrowseItem(instance("instance #14"), "DA 3890 A1 I72 41885"),
          cnBrowseItem(instance("instance #22"), "DA 3890 A2 B76 42002")
        ))),

      arguments(aroundIncludingQuery, firstAnchorCallNumber, 5, new CallNumberBrowseResult()
        .totalRecords(36).prev("CE 16 B6724 41993").next("DA 3700 C95 NO 18").items(List.of(
          cnBrowseItem(instance("instance #45"), "CE 16 B6724 41993"),
          cnBrowseItem(instance("instance #04"), "CE 16 D86 X 41998"),
          cnBrowseItem(instance("instance #38"), "CE 210 K297 41858", true),
          cnBrowseItem(instance("instance #36"), "DA 3700 B91 L79"),
          cnBrowseItem(instance("instance #09"), "DA 3700 C95 NO 18")
        ))),

      arguments(aroundIncludingQuery, secondAnchorCallNumber, 5, new CallNumberBrowseResult()
        .totalRecords(49).prev("DA 3880 O6 M81").next("DA 3890 A2 B76 42002").items(List.of(
          cnBrowseItem(instance("instance #13"), "DA 3880 O6 M81"),
          cnBrowseItem(instance("instance #02"), "DA 3880 O6 M96"),
          cnBrowseItem(0, "DA 3890 A1", true),
          cnBrowseItem(instance("instance #14"), "DA 3890 A1 I72 41885"),
          cnBrowseItem(instance("instance #22"), "DA 3890 A2 B76 42002")
        ))),

      // checks order of closely placed call-numbers
      arguments(aroundIncludingQuery, secondAnchorCallNumber, 30, new CallNumberBrowseResult()
        .totalRecords(49).prev("DA 3870 H47 41975").next("E 211 N52 VOL 14").items(List.of(
          cnBrowseItem(instance("instance #07"), "DA 3870 H47 41975"),
          cnBrowseItem(instance("instance #11"), "DA 3880 K56 M27 41984"),
          cnBrowseItem(instance("instance #32"), "DA 3880 O5 C3 V1"),
          cnBrowseItem(instance("instance #32"), "DA 3880 O5 C3 V2"),
          cnBrowseItem(instance("instance #32"), "DA 3880 O5 C3 V3"),
          cnBrowseItem(instance("instance #29"), "DA 3880 O6 D5"),
          cnBrowseItem(instance("instance #01"), "DA 3880 O6 J72"),
          cnBrowseItem(instance("instance #03"), "DA 3880 O6 L5 41955"),
          cnBrowseItem(instance("instance #06"), "DA 3880 O6 L6 V1"),
          cnBrowseItem(instance("instance #06"), "DA 3880 O6 L6 V2"),
          cnBrowseItem(instance("instance #20"), "DA 3880 O6 L75"),
          cnBrowseItem(instance("instance #15"), "DA 3880 O6 L76"),
          cnBrowseItem(instance("instance #05"), "DA 3880 O6 M15"),
          cnBrowseItem(instance("instance #13"), "DA 3880 O6 M81"),
          cnBrowseItem(instance("instance #02"), "DA 3880 O6 M96"),
          cnBrowseItem(0, "DA 3890 A1", true),
          cnBrowseItem(instance("instance #14"), "DA 3890 A1 I72 41885"),
          cnBrowseItem(instance("instance #22"), "DA 3890 A2 B76 42002"),
          cnBrowseItem(instance("instance #19"), "DA 3890 A2 F57 42011"),
          cnBrowseItem(instance("instance #24"), "DA 3900 C39 NO 11"),
          cnBrowseItem(instance("instance #34"), "DA 3900 C89 V1"),
          cnBrowseItem(instance("instance #34"), "DA 3900 C89 V2"),
          cnBrowseItem(instance("instance #34"), "DA 3900 C89 V3"),
          cnBrowseItem(instance("instance #28"), "DB 11 A31 BD 3124"),
          cnBrowseItem(instance("instance #23"), "DB 11 A66 SUPPL NO 11"),
          cnBrowseItem(instance("instance #10"), "DC 3201 B34 41972"),
          cnBrowseItem(instance("instance #35"), "E 12.11 I12 288 D"),
          cnBrowseItem(instance("instance #33"), "E 12.11 I2 298"),
          cnBrowseItem(instance("instance #27"), "E 211 A506"),
          cnBrowseItem(instance("instance #12"), "E 211 N52 VOL 14")
        ))),

      // checks if collapsing by the same result works correctly
      arguments(aroundIncludingQuery, "FC", 5, new CallNumberBrowseResult()
        .totalRecords(40).prev("FA 42010 3546 256").next("G 45831 S2").items(List.of(
          cnBrowseItem(instance("instance #43"), "FA 42010 3546 256"),
          cnBrowseItem(instance("instance #42"), "FA 46252 3977 12 237"),
          cnBrowseItem(0, "FC", true),
          cnBrowseItem(2, "FC 17 B89"),
          cnBrowseItem(instance("instance #31"), "G 45831 S2")
        ))),

      // checks if collapsing by the same result works correctly
      arguments(aroundIncludingQuery, "fc", 5, new CallNumberBrowseResult()
        .totalRecords(40).prev("FA 42010 3546 256").next("G 45831 S2").items(List.of(
          cnBrowseItem(instance("instance #43"), "FA 42010 3546 256"),
          cnBrowseItem(instance("instance #42"), "FA 46252 3977 12 237"),
          cnBrowseItem(0, "fc", true),
          cnBrowseItem(2, "FC 17 B89"),
          cnBrowseItem(instance("instance #31"), "G 45831 S2")
        ))),

      // browsing forward
      arguments(forwardQuery, firstAnchorShelfKey, 5, new CallNumberBrowseResult()
        .totalRecords(28).prev("DA 3700 B91 L79").next("DA 3880 K56 M27 41984").items(List.of(
          cnBrowseItem(instance("instance #36"), "DA 3700 B91 L79"),
          cnBrowseItem(instance("instance #09"), "DA 3700 C95 NO 18"),
          cnBrowseItem(instance("instance #41"), "DA 3870 B55 41868"),
          cnBrowseItem(instance("instance #07"), "DA 3870 H47 41975"),
          cnBrowseItem(instance("instance #11"), "DA 3880 K56 M27 41984")
        ))),

      arguments(forwardQuery, secondAnchorShelfKey, 5, new CallNumberBrowseResult()
        .totalRecords(25).prev("DA 3890 A1 I72 41885").next("DA 3900 C89 V1").items(List.of(
          cnBrowseItem(instance("instance #14"), "DA 3890 A1 I72 41885"),
          cnBrowseItem(instance("instance #22"), "DA 3890 A2 B76 42002"),
          cnBrowseItem(instance("instance #19"), "DA 3890 A2 F57 42011"),
          cnBrowseItem(instance("instance #24"), "DA 3900 C39 NO 11"),
          cnBrowseItem(instance("instance #34"), "DA 3900 C89 V1")
        ))),

      // checks if collapsing works in forward direction
      arguments(forwardQuery, "F", 5, new CallNumberBrowseResult()
        .totalRecords(14).prev("F  PR1866.S63 V.1 C.1").next("FC 17 B89").items(List.of(
          cnBrowseItem(instance("instance #46"), "F  PR1866.S63 V.1 C.1"),
          cnBrowseItem(instance("instance #27"), "F 43733 L370 41992"),
          cnBrowseItem(instance("instance #43"), "FA 42010 3546 256"),
          cnBrowseItem(instance("instance #42"), "FA 46252 3977 12 237"),
          cnBrowseItem(2, "FC 17 B89")
        ))),

      arguments(forwardQuery, "Z", 10, new CallNumberBrowseResult()
        .totalRecords(0).prev(null).next(null).items(emptyList())),

      arguments(forwardIncludingQuery, firstAnchorShelfKey, 5, new CallNumberBrowseResult()
        .totalRecords(28).prev("CE 210 K297 41858").next("DA 3870 H47 41975").items(List.of(
          cnBrowseItem(instance("instance #38"), "CE 210 K297 41858"),
          cnBrowseItem(instance("instance #36"), "DA 3700 B91 L79"),
          cnBrowseItem(instance("instance #09"), "DA 3700 C95 NO 18"),
          cnBrowseItem(instance("instance #41"), "DA 3870 B55 41868"),
          cnBrowseItem(instance("instance #07"), "DA 3870 H47 41975")
        ))),

      arguments(forwardIncludingQuery, secondAnchorShelfKey, 5, new CallNumberBrowseResult()
        .totalRecords(25).prev("DA 3890 A1 I72 41885").next("DA 3900 C89 V1").items(List.of(
          cnBrowseItem(instance("instance #14"), "DA 3890 A1 I72 41885"),
          cnBrowseItem(instance("instance #22"), "DA 3890 A2 B76 42002"),
          cnBrowseItem(instance("instance #19"), "DA 3890 A2 F57 42011"),
          cnBrowseItem(instance("instance #24"), "DA 3900 C39 NO 11"),
          cnBrowseItem(instance("instance #34"), "DA 3900 C89 V1")
        ))),

      // browsing backward
      arguments(backwardQuery, firstAnchorShelfKey, 5, new CallNumberBrowseResult()
        .totalRecords(8).prev("AC 11 A67 X 42000").next("CE 16 D86 X 41998").items(List.of(
          cnBrowseItem(instance("instance #08"), "AC 11 A67 X 42000"),
          cnBrowseItem(instance("instance #18"), "AC 11 E8 NO 14 P S1487"),
          cnBrowseItem(instance("instance #44"), "CE 16 B6713 X 41993"),
          cnBrowseItem(instance("instance #45"), "CE 16 B6724 41993"),
          cnBrowseItem(instance("instance #04"), "CE 16 D86 X 41998")
        ))),

      arguments(backwardQuery, secondAnchorShelfKey, 5, new CallNumberBrowseResult()
        .totalRecords(24).prev("DA 3880 O6 L75").next("DA 3880 O6 M96").items(List.of(
          cnBrowseItem(instance("instance #20"), "DA 3880 O6 L75"),
          cnBrowseItem(instance("instance #15"), "DA 3880 O6 L76"),
          cnBrowseItem(instance("instance #05"), "DA 3880 O6 M15"),
          cnBrowseItem(instance("instance #13"), "DA 3880 O6 M81"),
          cnBrowseItem(instance("instance #02"), "DA 3880 O6 M96")
        ))),

      // check that collapsing works for browsing backward
      arguments(backwardQuery, "G", 5, new CallNumberBrowseResult()
        .totalRecords(32).prev("F  PR1866.S63 V.1 C.1").next("FC 17 B89").items(List.of(
          cnBrowseItem(instance("instance #46"), "F  PR1866.S63 V.1 C.1"),
          cnBrowseItem(instance("instance #27"), "F 43733 L370 41992"),
          cnBrowseItem(instance("instance #43"), "FA 42010 3546 256"),
          cnBrowseItem(instance("instance #42"), "FA 46252 3977 12 237"),
          cnBrowseItem(2, "FC 17 B89")
        ))),

      arguments(backwardQuery, "F 11", 5, new CallNumberBrowseResult()
        .totalRecords(28).prev("E 12.11 I12 288 D").next("F  PR1866.S63 V.1 C.1").items(List.of(
          cnBrowseItem(instance("instance #35"), "E 12.11 I12 288 D"),
          cnBrowseItem(instance("instance #33"), "E 12.11 I2 298"),
          cnBrowseItem(instance("instance #27"), "E 211 A506"),
          cnBrowseItem(instance("instance #12"), "E 211 N52 VOL 14"),
          cnBrowseItem(instance("instance #46"), "F  PR1866.S63 V.1 C.1")
        ))),

      arguments(backwardQuery, "A", 10, new CallNumberBrowseResult()
        .totalRecords(0).prev(null).next(null).items(emptyList())),

      arguments(backwardIncludingQuery, firstAnchorShelfKey, 5, new CallNumberBrowseResult()
        .totalRecords(8).prev("AC 11 E8 NO 14 P S1487").next("CE 210 K297 41858").items(List.of(
          cnBrowseItem(instance("instance #18"), "AC 11 E8 NO 14 P S1487"),
          cnBrowseItem(instance("instance #44"), "CE 16 B6713 X 41993"),
          cnBrowseItem(instance("instance #45"), "CE 16 B6724 41993"),
          cnBrowseItem(instance("instance #04"), "CE 16 D86 X 41998"),
          cnBrowseItem(instance("instance #38"), "CE 210 K297 41858")
        ))),

      arguments(backwardIncludingQuery, secondAnchorShelfKey, 5, new CallNumberBrowseResult()
        .totalRecords(24).prev("DA 3880 O6 L75").next("DA 3880 O6 M96").items(List.of(
          cnBrowseItem(instance("instance #20"), "DA 3880 O6 L75"),
          cnBrowseItem(instance("instance #15"), "DA 3880 O6 L76"),
          cnBrowseItem(instance("instance #05"), "DA 3880 O6 M15"),
          cnBrowseItem(instance("instance #13"), "DA 3880 O6 M81"),
          cnBrowseItem(instance("instance #02"), "DA 3880 O6 M96")
        )))
    );
  }

  private static Instance[] instances() {
    return callNumberBrowseInstanceData().stream()
      .map(BrowseCallNumberIT::instance)
      .toArray(Instance[]::new);
  }

  @SuppressWarnings("unchecked")
  private static Instance instance(List<Object> data) {
    var items = ((List<String>) data.get(1)).stream()
      .map(callNumber -> new Item()
        .tenantId(TENANT_ID)
        .id(randomId())
        .discoverySuppress(false)
        .effectiveCallNumberComponents(new ItemEffectiveCallNumberComponents().callNumber(callNumber))
        .effectiveShelvingOrder(getShelfKeyFromCallNumber(callNumber))
      )
      .toList();

    return new Instance()
      .id(randomId())
      .title((String) data.get(0))
      .staffSuppress(false)
      .discoverySuppress(false)
      .isBoundWith(false)
      .shared(false)
      .tenantId(TENANT_ID)
      .items(items)
      .holdings(emptyList());
  }

  private static Instance instance(String title) {
    return INSTANCE_MAP.get(title);
  }

  private static List<List<Object>> callNumberBrowseInstanceData() {
    return List.of(
      List.of("instance #01", List.of("DA 3880 O6 J72")),
      List.of("instance #02", List.of("DA 3880 O6 M96")),
      List.of("instance #03", List.of("DA 3880 O6 L5 41955")),
      List.of("instance #04", List.of("CE 16 D86 X 41998")),
      List.of("instance #05", List.of("DA 3880 O6 M15")),
      List.of("instance #06", List.of("DA 3880 O6 L6 V1", "DA 3880 O6 L6 V2", "DA 3880 O6 L6 V2")),
      List.of("instance #07", List.of("DA 3870 H47 41975")),
      List.of("instance #08", List.of("AC 11 A67 X 42000")),
      List.of("instance #09", List.of("DA 3700 C95 NO 18")),
      List.of("instance #10", List.of("DC 3201 B34 41972")),
      List.of("instance #11", List.of("DA 3880 K56 M27 41984")),
      List.of("instance #12", List.of("E 211 N52 VOL 14")),
      List.of("instance #13", List.of("DA 3880 O6 M81")),
      List.of("instance #14", List.of("DA 3890 A1 I72 41885")),
      List.of("instance #15", List.of("DA 3880 O6 L76")),
      List.of("instance #16", List.of("PR 44034 B38 41993")),
      List.of("instance #17", List.of("GA 16 A63 41581")),
      List.of("instance #18", List.of("AC 11 E8 NO 14 P S1487")),
      List.of("instance #19", List.of("DA 3890 A2 F57 42011")),
      List.of("instance #20", List.of("DA 3880 O6 L75")),
      List.of("instance #21", List.of("FC 17 B89")),
      List.of("instance #22", List.of("DA 3890 A2 B76 42002")),
      List.of("instance #23", List.of("DB 11 A66 SUPPL NO 11")),
      List.of("instance #24", List.of("DA 3900 C39 NO 11")),
      List.of("instance #25", List.of("AC 11 A4 VOL 235")),
      List.of("instance #26", List.of("PR 17 I55 42006")),
      List.of("instance #27", List.of("E 211 A506", "F 43733 L370 41992")),
      List.of("instance #28", List.of("DB 11 A31 BD 3124")),
      List.of("instance #29", List.of("DA 3880 O6 D5")),
      List.of("instance #30", List.of("GA 16 G32 41557 V1", "GA 16 G32 41557 V2", "GA 16 G32 41557 V3")),
      List.of("instance #31", List.of("AB 14 C72 NO 220", "G 45831 S2")),
      List.of("instance #32", List.of("DA 3880 O5 C3 V1", "DA 3880 O5 C3 V2", "DA 3880 O5 C3 V3")),
      List.of("instance #33", List.of("E 12.11 I2 298")),
      List.of("instance #34", List.of("DA 3900 C89 V1", "DA 3900 C89 V2", "DA 3900 C89 V3")),
      List.of("instance #35", List.of("E 12.11 I12 288 D")),
      List.of("instance #36", List.of("DA 3700 B91 L79")),
      List.of("instance #37", List.of("FC 17 B89")),
      List.of("instance #38", List.of("CE 210 K297 41858")),
      List.of("instance #39", List.of("GA 16 D64 41548A")),
      List.of("instance #40", List.of("PR 213 E5 41999")),
      List.of("instance #41", List.of("DA 3870 B55 41868")),
      List.of("instance #42", List.of("FA 46252 3977 12 237")),
      List.of("instance #43", List.of("FA 42010 3546 256")),
      List.of("instance #44", List.of("CE 16 B6713 X 41993")),
      List.of("instance #45", List.of("CE 16 B6724 41993")),
      List.of("instance #47", List.of("J29.29:M54/990")),
      List.of("instance #46", List.of("F  PR1866.S63 V.1 C.1"))
    );
  }
}
