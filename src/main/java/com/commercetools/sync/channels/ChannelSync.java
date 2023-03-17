package com.commercetools.sync.channels;

import com.commercetools.sync.channels.helpers.ChannelBatchValidator;
import com.commercetools.sync.channels.helpers.ChannelSyncStatistics;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.impl.ChannelServiceImpl;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.commands.UpdateAction;

import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.channels.utils.ChannelSyncUtils.buildActions;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class ChannelSync
    extends BaseSync<
        ChannelDraft, Channel, ChannelSyncStatistics, ChannelSyncOptions> {

  private static final String CHANNEL_FETCH_FAILED =
      "Failed to fetch existing channels with keys: '%s'.";
  private static final String CHANNEL_UPDATE_FAILED =
      "Failed to update channel with key: '%s'. Reason: %s";

  private final ChannelService channelService;
  private final ChannelBatchValidator batchValidator;

  public ChannelSync(@Nonnull final ChannelSyncOptions channelSyncOptions) {
    this(channelSyncOptions, new ChannelServiceImpl(channelSyncOptions, Set.of()));
  }

  /**
   * Takes a {@link ChannelSyncOptions} and a {@link ChannelSync} instances to instantiate a
   * new {@link ChannelSync} instance that could be used to sync tax category drafts in the CTP
   * project specified in the injected {@link ChannelSyncOptions} instance.
   *
   * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and
   * passed to.
   *
   * @param channelSyncOptions the container of all the options of the sync process including
   *     the CTP project client and/or configuration and other sync-specific options.
   * @param channelService the tax category service which is responsible for fetching/caching
   *     the tax categories from the CTP project.
   */
  ChannelSync(
      @Nonnull final ChannelSyncOptions channelSyncOptions,
      @Nonnull final ChannelService channelService) {
    super(new ChannelSyncStatistics(), channelSyncOptions);
    this.channelService = channelService;
    this.batchValidator = new ChannelBatchValidator(getSyncOptions(), getStatistics());
  }

  @Override
  protected CompletionStage<ChannelSyncStatistics> process(
      @Nonnull final List<ChannelDraft> resourceDrafts) {
    List<List<ChannelDraft>> batches =
        batchElements(resourceDrafts, syncOptions.getBatchSize());
    return syncBatches(batches, completedFuture(statistics));
  }

  /**
   * This method first creates a new {@link Set} of valid {@link ChannelDraft} elements. For
   * more on the rules of validation, check: {@link
   * ChannelBatchValidator#validateAndCollectReferencedKeys(List)}. Using the resulting set of
   * {@code validChannelDrafts}, the matching tax categories in the target CTP project are
   * fetched then the method {@link ChannelSync#syncBatch(Set, Set)} is called to perform the
   * sync (<b>update</b> or <b>create</b> requests accordingly) on the target project.
   *
   * <p>In case of error during of fetching of existing tax categories, the error callback will be
   * triggered. And the sync process would stop for the given batch.
   *
   * @param batch batch of drafts that need to be synced
   * @return a {@link CompletionStage} containing an instance of {@link ChannelSyncStatistics}
   *     which contains information about the result of syncing the supplied batch to the target
   *     project.
   */
  @Override
  protected CompletionStage<ChannelSyncStatistics> processBatch(
      @Nonnull final List<ChannelDraft> batch) {

    final ImmutablePair<Set<ChannelDraft>, Set<String>> result =
        batchValidator.validateAndCollectReferencedKeys(batch);

    final Set<ChannelDraft> validDrafts = result.getLeft();
    if (validDrafts.isEmpty()) {
      statistics.incrementProcessed(batch.size());
      return CompletableFuture.completedFuture(statistics);
    }
    final Set<String> validChannelKeys = result.getRight();

    return channelService
        .fetchMatchingChannelsByKeys(validChannelKeys)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              Set<Channel> fetchedChannels = fetchResponse.getKey();
              final Throwable exception = fetchResponse.getValue();

              if (exception != null) {
                final String errorMessage = format(CHANNEL_FETCH_FAILED, validChannelKeys);
                handleError(errorMessage, exception, null, null, null, validChannelKeys.size());
                return completedFuture(null);
              } else {
                return syncBatch(fetchedChannels, validDrafts);
              }
            })
        .thenApply(
            ignored -> {
              statistics.incrementProcessed(batch.size());
              return statistics;
            });
  }

  /**
   * Given a set of tax category drafts, attempts to sync the drafts with the existing tax
   * categories in the CTP project. The tax category and the draft are considered to match if they
   * have the same key. When there will be no error it will attempt to sync the drafts transactions.
   *
   * @param oldChannels old tax categories.
   * @param newChannels drafts that need to be synced.
   * @return a {@link CompletionStage} which contains an empty result after execution of the update
   */
  @Nonnull
  private CompletionStage<Void> syncBatch(
      @Nonnull final Set<Channel> oldChannels,
      @Nonnull final Set<ChannelDraft> newChannels) {

    final Map<String, Channel> oldChannelMap =
            oldChannels.stream().collect(toMap(Channel::getKey, identity()));

    return allOf(
            newChannels.stream()
            .map(
                newChannel -> {
                  final Channel oldChannel = oldChannelMap.get(newChannel.getKey());

                  return ofNullable(oldChannel)
                      .map(channel -> buildActionsAndUpdate(oldChannel, newChannel))
                      .orElseGet(() -> applyCallbackAndCreate(newChannel));
                })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  /**
   * Given a tax category draft, this method applies the beforeCreateCallback and then issues a
   * create request to the CTP project to create the corresponding Channel.
   *
   * @param channelDraft the tax category draft to create the tax category from.
   * @return a {@link CompletionStage} which contains an empty result after execution of the create.
   */
  @Nonnull
  private CompletionStage<Optional<Channel>> applyCallbackAndCreate(
      @Nonnull final ChannelDraft channelDraft) {

    return syncOptions
        .applyBeforeCreateCallback(channelDraft)
        .map(
            draft ->
                channelService
                    .createChannel(draft)
                    .thenApply(
                        channelOptional -> {
                          if (channelOptional.isPresent()) {
                            statistics.incrementCreated();
                          } else {
                            statistics.incrementFailed();
                          }
                          return channelOptional;
                        }))
        .orElse(completedFuture(Optional.empty()));
  }

  @Nonnull
  private CompletionStage<Optional<Channel>> buildActionsAndUpdate(
      @Nonnull final Channel oldChannel, @Nonnull final ChannelDraft newChannel) {

    final List<UpdateAction<Channel>> updateActions =
        buildActions(oldChannel, newChannel);

    List<UpdateAction<Channel>> updateActionsAfterCallback =
        syncOptions.applyBeforeUpdateCallback(updateActions, newChannel, oldChannel);

    if (!updateActionsAfterCallback.isEmpty()) {
      return updateChannel(oldChannel, newChannel, updateActionsAfterCallback);
    }

    return completedFuture(null);
  }

  /**
   * Given an existing {@link Channel} and a new {@link ChannelDraft}, the method calculates
   * all the update actions required to synchronize the existing tax category to be the same as the
   * new one. If there are update actions found, a request is made to CTP to update the existing tax
   * category, otherwise it doesn't issue a request.
   *
   * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was
   * carried out successfully or not. If an exception was thrown on executing the request to CTP,
   * the error handling method is called.
   *
   * @param oldChannel existing tax category that could be updated.
   * @param newChannel draft containing data that could differ from data in {@code
   *     oldChannel}.
   * @return a {@link CompletionStage} which contains an empty result after execution of the update.
   */
  @Nonnull
  private CompletionStage<Optional<Channel>> updateChannel(
      @Nonnull final Channel oldChannel,
      @Nonnull final ChannelDraft newChannel,
      @Nonnull final List<UpdateAction<Channel>> updateActions) {

    return channelService
        .updateChannel(oldChannel, updateActions)
        .handle(ImmutablePair::new)
        .thenCompose(
            updateResponse -> {
              final Channel updatedChannel = updateResponse.getKey();
              final Throwable sphereException = updateResponse.getValue();

              if (sphereException != null) {
                return executeSupplierIfConcurrentModificationException(
                    sphereException,
                    () -> fetchAndUpdate(oldChannel, newChannel),
                    () -> {
                      final String errorMessage =
                          format(
                              CHANNEL_UPDATE_FAILED,
                              newChannel.getKey(),
                              sphereException.getMessage());
                      handleError(
                          errorMessage,
                          sphereException,
                          oldChannel,
                          newChannel,
                          updateActions,
                          1);
                      return completedFuture(Optional.empty());
                    });
              } else {
                statistics.incrementUpdated();
                return completedFuture(Optional.of(updatedChannel));
              }
            });
  }

  @Nonnull
  private CompletionStage<Optional<Channel>> fetchAndUpdate(
      @Nonnull final Channel oldChannel, @Nonnull final ChannelDraft newChannel) {

    final String key = oldChannel.getKey();
    return channelService
        .fetchChannelByKey(key)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Optional<Channel> fetchedChannelOptional = fetchResponse.getKey();
              final Throwable exception = fetchResponse.getValue();

              if (exception != null) {
                final String errorMessage =
                    format(
                        CHANNEL_UPDATE_FAILED,
                        key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                handleError(errorMessage, exception, oldChannel, newChannel, null, 1);
                return completedFuture(null);
              }

              return fetchedChannelOptional
                  .map(
                      fetchedChannel ->
                          buildActionsAndUpdate(fetchedChannel, newChannel))
                  .orElseGet(
                      () -> {
                        final String errorMessage =
                            format(
                                CHANNEL_UPDATE_FAILED,
                                key,
                                "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                        handleError(errorMessage, null, oldChannel, newChannel, null, 1);
                        return completedFuture(null);
                      });
            });
  }
}
