package com.commercetools.sync.channels;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;

public final class ChannelSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        ChannelSyncOptionsBuilder,
        ChannelSyncOptions,
        Channel,
        ChannelDraft,
        Channel> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private ChannelSyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link ChannelSyncOptionsBuilder} given a {@link SphereClient}
   * responsible for interaction with the target CTP project, with the default batch size ({@code
   * BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link SphereClient} responsible for interaction with the
   *     target CTP project.
   * @return new instance of {@link ChannelSyncOptionsBuilder}
   */
  public static ChannelSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
    return new ChannelSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link ChannelSyncOptions} enriched with all attributes provided to
   * {@code this} builder.
   *
   * @return new instance of {@link ChannelSyncOptions}
   */
  @Override
  public ChannelSyncOptions build() {
    return new ChannelSyncOptions(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }

  /**
   * Returns an instance of this class to be used in the superclass's generic methods. Please see
   * the JavaDoc in the overridden method for further details.
   *
   * @return an instance of this class.
   */
  @Override
  protected ChannelSyncOptionsBuilder getThis() {
    return this;
  }
}
