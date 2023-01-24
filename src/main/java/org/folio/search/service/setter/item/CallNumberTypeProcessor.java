package org.folio.search.service.setter.item;

import org.apache.commons.lang3.StringUtils;
import org.folio.search.domain.dto.Instance;
import org.folio.search.domain.dto.Item;
import org.folio.search.service.setter.FieldProcessor;
import org.marc4j.callnum.DeweyCallNumber;
import org.marc4j.callnum.LCCallNumber;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.folio.search.utils.CollectionUtils.toStreamSafe;

@Component
public class CallNumberTypeProcessor implements FieldProcessor<Instance, Set<String>> {

  @Override
  public Set<String> getFieldValue(Instance instance) {
    return toStreamSafe(instance.getItems())
      .map(Item::getEffectiveCallNumberComponents)
      .filter(Objects::nonNull)
      .map(cn -> toType(cn.getCallNumber()))
      .filter(StringUtils::isNotBlank)
      .collect(toSet());
  }

  private String toType(String cn) {
    if (new LCCallNumber(cn).isValid()) {
      return "LC";
    }
    if (new DeweyCallNumber(cn).isValid()) {
      return "Dewey";
    }
    return "Local";
  }
}
