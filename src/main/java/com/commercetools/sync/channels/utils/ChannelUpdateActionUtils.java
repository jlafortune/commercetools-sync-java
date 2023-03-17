package com.commercetools.sync.channels.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.channels.commands.updateactions.ChangeName;
import io.sphere.sdk.channels.commands.updateactions.ChangeDescription;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public final class ChannelUpdateActionUtils {

  private ChannelUpdateActionUtils() {}

  /**
   * Compares the {@code name} values of a {@link Channel} and a {@link ChannelDraft} and
   * returns an {@link Optional} of update action, which would contain the {@code "changeName"}
   * {@link UpdateAction}. If both {@link Channel} and {@link ChannelDraft} have the same
   * {@code name} values, then no update action is needed and empty optional will be returned.
   *
   * @param oldChannel the tax category that should be updated.
   * @param newChannel the tax category draft which contains the new name.
   * @return optional containing update action or empty optional if names are identical.
   */
  @Nonnull
  public static Optional<UpdateAction<Channel>> buildChangeNameAction(
      @Nonnull final Channel oldChannel, @Nonnull final ChannelDraft newChannel) {

    return buildUpdateAction(
        oldChannel.getName(),
        newChannel.getName(),
        () -> ChangeName.of(newChannel.getName()));
  }

  /**
   * Compares the {@code description} values of a {@link Channel} and a {@link ChannelDraft}
   * and returns an {@link Optional} of update action, which would contain the {@code
   * "setDescription"} {@link UpdateAction}. If both {@link Channel} and {@link
   * ChannelDraft} have the same {@code description} values, then no update action is needed and
   * empty optional will be returned.
   *
   * @param oldChannel the tax category that should be updated.
   * @param newChannel the tax category draft which contains the new description.
   * @return optional containing update action or empty optional if descriptions are identical.
   */
  @Nonnull
  public static Optional<UpdateAction<Channel>> buildSetDescriptionAction(
      @Nonnull final Channel oldChannel, @Nonnull final ChannelDraft newChannel) {

    return buildUpdateAction(
        oldChannel.getDescription(),
        newChannel.getDescription(),
        () -> ChangeDescription.of(newChannel.getDescription()));
  }
}
