package org.folio.search.service;

import static java.lang.Boolean.parseBoolean;

import java.util.Collection;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.search.configuration.properties.SearchConfigurationProperties;
import org.folio.search.domain.dto.LanguageConfig;
import org.folio.search.domain.dto.ReindexRequest;
import org.folio.search.service.browse.CallNumberBrowseRangeService;
import org.folio.search.service.metadata.ResourceDescriptionService;
import org.folio.search.service.systemuser.SystemUserService;
import org.folio.spring.FolioExecutionContext;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class SearchTenantService {

  private static final String REINDEX_PARAM_NAME = "runReindex";

  private final IndexService indexService;
  private final FolioExecutionContext context;
  private final SystemUserService systemUserService;
  private final LanguageConfigService languageConfigService;
  private final CallNumberBrowseRangeService callNumberBrowseRangeService;
  private final ResourceDescriptionService resourceDescriptionService;
  private final SearchConfigurationProperties searchConfigurationProperties;

  /**
   * Initializes tenant using given {@link TenantAttributes}.
   *
   * <p>This method:</p>
   * <ul>
   *   <li>Creates a system user to perform record indexing</li>
   *   <li>Add default languages to the tenant configuration</li>
   *   <li>Creates Elasticsearch index and corresponding mappings for supported record types</li>
   *   <li>Starts reindexing process for inventory (if it's specified)</li>
   * </ul>
   *
   * @param tenantAttributes - tenant attributes comes from initialization request.
   */
  public void initializeTenant(TenantAttributes tenantAttributes) {
    systemUserService.prepareSystemUser();

    var existingLanguages = languageConfigService.getAllLanguageCodes();

    var initialLanguages = searchConfigurationProperties.getInitialLanguages();
    log.info("Initializing tenant [initialLanguages={}, existingLanguages={}]", initialLanguages, existingLanguages);

    initialLanguages.stream()
      .filter(code -> !existingLanguages.contains(code))
      .map(code -> new LanguageConfig().code(code))
      .forEach(languageConfigService::create);

    var resourceNames = resourceDescriptionService.getResourceNames();
    resourceNames.forEach(resourceName -> indexService.createIndexIfNotExist(resourceName, context.getTenantId()));
    Stream.ofNullable(tenantAttributes.getParameters())
      .flatMap(Collection::stream)
      .filter(parameter -> parameter.getKey().equals(REINDEX_PARAM_NAME) && parseBoolean(parameter.getValue()))
      .findFirst()
      .ifPresent(parameter -> resourceNames.forEach(resource -> {
        if (resourceDescriptionService.get(resource).getParent() == null) {
          indexService.reindexInventory(context.getTenantId(), new ReindexRequest().resourceName(resource));
        }
      }));
  }

  /**
   * Disables tenant by removes elasticsearch indices for all supported record types and cleaning related caches.
   */
  public void disableTenant() {
    var tenantId = context.getTenantId();
    callNumberBrowseRangeService.evictRangeCache(tenantId);
    resourceDescriptionService.getResourceNames().forEach(name -> {
      log.info("Removing elasticsearch index [resourceName={}, tenant={}]", name, tenantId);
      indexService.dropIndex(name, tenantId);
    });
  }
}
