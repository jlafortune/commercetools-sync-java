package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.services.ChannelService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelUpdateCommand;
import io.sphere.sdk.channels.expansion.ChannelExpansionModel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.channels.queries.ChannelQueryBuilder;
import io.sphere.sdk.channels.queries.ChannelQueryModel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQueryBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static io.sphere.sdk.products.ProductProjectionType.STAGED;

public final class ChannelServiceImpl
    extends BaseServiceWithKey<
        ChannelDraft,
        Channel,
        Channel,
        BaseSyncOptions,
        ChannelQuery,
        ChannelQueryModel,
        ChannelExpansionModel<Channel>>
    implements ChannelService {

  private final Set<ChannelRole> channelRoles;

  public ChannelServiceImpl(
      @Nonnull final BaseSyncOptions syncOptions, @Nonnull final Set<ChannelRole> channelRoles) {
    super(syncOptions);
    this.channelRoles = channelRoles;
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> channelKeys) {

    return cacheKeysToIds(
        channelKeys,
        keysNotCached ->
            new ResourceKeyIdGraphQlRequest(keysNotCached, GraphQlQueryResources.CHANNELS));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedChannelId(@Nonnull final String key) {

    return fetchCachedResourceId(
        key,
        () ->
            ChannelQueryBuilder.of()
                .plusPredicates(queryModel -> queryModel.key().is(key))
                .build());
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Channel>> createChannel(@Nonnull final String key) {

    final ChannelDraft draft = ChannelDraftBuilder.of(key).roles(channelRoles).build();

    return createResource(draft, ChannelCreateCommand::of);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Channel>> createAndCacheChannel(@Nonnull final String key) {

    return createChannel(key)
        .thenApply(
            channelOptional -> {
              channelOptional.ifPresent(channel -> keyToIdCache.put(key, channel.getId()));
              return channelOptional;
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Channel>> fetchChannelByKey(@Nullable final String key) {

    return fetchResource(
            key,
            () ->
                    ChannelQueryBuilder.of()
                            .plusPredicates(queryModel -> queryModel.key().is(key))
                            .build());
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Channel>> createChannel(
          @Nonnull final ChannelDraft channelDraft) {

    return createResource(channelDraft, ChannelCreateCommand::of)
            .thenApply(channel -> channel);
  }

  @Nonnull
  @Override
  public CompletionStage<Channel> updateChannel(
          @Nonnull final Channel channel, @Nonnull final List<UpdateAction<Channel>> updateActions) {
    return updateResource(channel, ChannelUpdateCommand::of, updateActions);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<Channel>> fetchMatchingChannelsByKeys(
          @Nonnull final Set<String> keys) {
    return fetchMatchingResources(
            keys,
            (keysNotCached) ->
                    ChannelQueryBuilder.of()
                            .plusPredicates(queryModel -> queryModel.key().isIn(keysNotCached))
                            .build());
  }
}
