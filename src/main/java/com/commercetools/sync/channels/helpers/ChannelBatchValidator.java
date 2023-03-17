package com.commercetools.sync.channels.helpers;

import com.commercetools.sync.channels.ChannelSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ChannelBatchValidator
    extends BaseBatchValidator<
        ChannelDraft, ChannelSyncOptions, ChannelSyncStatistics> {

  static final String CHANNEL_DRAFT_KEY_NOT_SET =
      "ChannelDraft with name: %s doesn't have a key. "
          + "Please make sure all tax category drafts have keys.";
  static final String CHANNEL_DRAFT_IS_NULL = "ChannelDraft is null.";
  static final String TAX_CATEGORY_DUPLICATED_COUNTRY =
      "Tax rate drafts have duplicated country "
          + "codes. Duplicated tax rate country code: '%s'. Tax rate country codes and "
          + "states are expected to be unique inside their tax category.";
  static final String TAX_CATEGORY_DUPLICATED_COUNTRY_AND_STATE =
      "Tax rate drafts have duplicated country "
          + "codes and states. Duplicated tax rate country code: '%s'. state : '%s'. Tax rate country codes and "
          + "states are expected to be unique inside their tax category.";

  public ChannelBatchValidator(
      @Nonnull final ChannelSyncOptions syncOptions,
      @Nonnull final ChannelSyncStatistics syncStatistics) {
    super(syncOptions, syncStatistics);
  }

  /**
   * Given the {@link List}&lt;{@link ChannelDraft}&gt; of drafts this method attempts to
   * validate drafts and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link
   * ChannelDraft}&gt;,{@link Set}&lt; {@link String}&gt;&gt; which contains the {@link Set} of
   * valid drafts and valid tax category keys.
   *
   * <p>A valid tax category draft is one which satisfies the following conditions:
   *
   * <ol>
   *   <li>It is not null
   *   <li>It has a key which is not blank (null/empty)
   *   <li>Tax rates have not duplicated country and state.
   * </ol>
   *
   * @param channelDrafts the tax category drafts to validate and collect valid tax category
   *     keys.
   * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link ChannelDraft}&gt;, {@link
   *     Set}&lt;{@link String}&gt;&gt; which contains the {@link Set} of valid drafts and valid tax
   *     category keys.
   */
  @Override
  public ImmutablePair<Set<ChannelDraft>, Set<String>> validateAndCollectReferencedKeys(
      @Nonnull final List<ChannelDraft> channelDrafts) {

    final Set<ChannelDraft> validDrafts =
        channelDrafts.stream().filter(this::isValidChannelDraft).collect(toSet());

    final Set<String> validKeys =
        validDrafts.stream().map(ChannelDraft::getKey).collect(toSet());

    return ImmutablePair.of(validDrafts, validKeys);
  }

  private boolean isValidChannelDraft(@Nullable final ChannelDraft channelDraft) {

    if (channelDraft == null) {
      handleError(CHANNEL_DRAFT_IS_NULL);
    } else if (isBlank(channelDraft.getKey())) {
      handleError(format(CHANNEL_DRAFT_KEY_NOT_SET, channelDraft.getName()));
    } else {
      return true;
    }

    return false;
  }

}
