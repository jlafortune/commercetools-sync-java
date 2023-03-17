package com.commercetools.sync.channels;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ChannelSyncOptions
    extends BaseSyncOptions<Channel, ChannelDraft, Channel> {

    ChannelSyncOptions(
      @Nonnull final SphereClient ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<ChannelDraft>,
                  Optional<Channel>,
                  List<UpdateAction<Channel>>>
              errorCallBack,
      @Nullable
          final TriConsumer<SyncException, Optional<ChannelDraft>, Optional<Channel>>
              warningCallBack,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<UpdateAction<Channel>>,
                  ChannelDraft,
                  Channel,
                  List<UpdateAction<Channel>>>
              beforeUpdateCallback,
      @Nullable final Function<ChannelDraft, ChannelDraft> beforeCreateCallback,
      final long cacheSize) {
    super(
        ctpClient,
        errorCallBack,
        warningCallBack,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }
}
