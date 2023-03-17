package com.commercetools.sync.channels.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

public final class ChannelSyncUtils {

  private ChannelSyncUtils() {}

  /**
   * Compares all the fields of a {@link Channel} and a {@link ChannelDraft}. It returns a
   * {@link List} of {@link UpdateAction}&lt;{@link Channel}&gt; as a result. If no update
   * action is needed, for example in case where both the {@link Channel} and the {@link
   * ChannelDraft} have the same fields, an empty {@link List} is returned.
   *
   * @param oldChannel the {@link Channel} which should be updated.
   * @param newChannel the {@link ChannelDraft} where we get the new data.
   * @return A list of tax category-specific update actions.
   */
  @Nonnull
  public static List<UpdateAction<Channel>> buildActions(
      @Nonnull final Channel oldChannel, @Nonnull final ChannelDraft newChannel) {

    final List<UpdateAction<Channel>> updateActions =
        new ArrayList<>(
            filterEmptyOptionals(
                // buildChangeNameAction(oldChannel, newChannel),
                // buildSetDescriptionAction(oldChannel, newChannel)
            )
                    );

    // updateActions.addAll(buildTaxRateUpdateActions(oldChannel, newChannel));

    return updateActions;
  }
}
