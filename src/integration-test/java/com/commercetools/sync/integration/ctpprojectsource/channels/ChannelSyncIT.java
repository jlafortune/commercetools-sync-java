package com.commercetools.sync.integration.ctpprojectsource.channels;

import com.commercetools.api.models.channel.ChannelRoleEnum;
import com.commercetools.sync.channels.ChannelSync;
import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.channels.ChannelSync;
import com.commercetools.sync.channels.ChannelSyncOptions;
import com.commercetools.sync.channels.ChannelSyncOptionsBuilder;
import com.commercetools.sync.channels.helpers.ChannelSyncStatistics;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.commands.CustomerDeleteCommand;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.GeoJSON;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ChannelITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StoreITUtils.createStore;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ChannelSyncIT {
  private List<String> errorMessages;
  private List<Throwable> exceptions;
  private ChannelSync channelSync;
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  @BeforeEach
  void setup() {
    deleteChannelSyncTestDataFromProjects();

    createSampleChannel(CTP_SOURCE_CLIENT, "Inventory Channel", ChannelRole.INVENTORY_SUPPLY);
    createSampleChannel(CTP_SOURCE_CLIENT, "Pricing Channel", ChannelRole.PRODUCT_DISTRIBUTION);

    createSampleChannel(CTP_TARGET_CLIENT, "Inventory Channel", ChannelRole.INVENTORY_SUPPLY);

    setUpChannelSync();
  }

  void createSampleChannel(@Nonnull final SphereClient ctpClient, @Nonnull final String description, ChannelRole ... roles) {
    final LocalizedString name = LocalizedString.of(Locale.forLanguageTag("en"), description);
    final ChannelDraft channelDraft =
            ChannelDraftBuilder.of(description.replace(" ", "-")).name(name).description(name)
                    .roles(Set.of(roles))
                    .build();

    System.out.println(channelDraft);
    executeBlocking(ctpClient.execute(ChannelCreateCommand.of(channelDraft)));
  }

  @AfterAll
  static void tearDown() {
    deleteChannelSyncTestDataFromProjects();
  }

  private static void deleteChannelSyncTestDataFromProjects() {
    queryAndExecute(CTP_SOURCE_CLIENT, ChannelQuery.of(), ChannelDeleteCommand::of);
    queryAndExecute(CTP_TARGET_CLIENT, ChannelQuery.of(), ChannelDeleteCommand::of);
  }

  private void setUpChannelSync() {
    errorMessages = new ArrayList<>();
    exceptions = new ArrayList<>();
    final ChannelSyncOptions channelSyncOptions =
        ChannelSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();
    channelSync = new ChannelSync(channelSyncOptions);
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  @Test
  void sync_WithoutUpdates_ShouldReturnProperStatistics() {

    final List<Channel> channels =
        CTP_SOURCE_CLIENT.execute(ChannelQuery.of()).toCompletableFuture().join().getResults();

  System.out.println(channels);

    final List<ChannelDraft> channelDrafts = channels.stream().map(channel -> ChannelDraftBuilder.of(channel.getKey())
            .address(channel.getAddress())
            .roles(channel.getRoles())
            .description(channel.getDescription())
            .name(channel.getName())
            .geoLocation(channel.getGeoLocation())
            .custom(channel.getCustom() == null ? null : CustomFieldsDraft.ofCustomFields(channel.getCustom()))
            .build()).collect(Collectors.toList());


    final ChannelSyncStatistics channelSyncStatistics =
        channelSync.sync(channelDrafts).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    AssertionsForStatistics.assertThat(channelSyncStatistics).hasValues(2, 1, 0, 0);
    assertThat(channelSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 channels were processed in total (1 created, 0 updated and 0 failed to sync).");
  }
//
//  @Test
//  void sync_WithUpdates_ShouldReturnProperStatistics() {
//
//    final List<Channel> channels =
//        CTP_SOURCE_CLIENT.execute(ChannelQuery.of()).toCompletableFuture().join().getResults();
//
//    final List<ChannelDraft> updatedChannelDrafts = prepareUpdatedChannelDrafts(channels);
//    final ChannelSyncStatistics channelSyncStatistics =
//        channelSync.sync(updatedChannelDrafts).toCompletableFuture().join();
//
//    assertThat(errorMessages).isEmpty();
//    assertThat(exceptions).isEmpty();
//
//    AssertionsForStatistics.assertThat(channelSyncStatistics).hasValues(2, 1, 1, 0);
//    assertThat(channelSyncStatistics.getReportMessage())
//        .isEqualTo(
//            "Summary: 2 channels were processed in total (1 created, 1 updated and 0 failed to sync).");
//  }
//
//  private List<ChannelDraft> prepareUpdatedChannelDrafts(
//      @Nonnull final List<Channel> channels) {
//
//    final Store storeCologne = createStore(CTP_TARGET_CLIENT, "store-cologne");
//
//    final List<ChannelDraft> channelDrafts =
//        ChannelTransformUtils.toChannelDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, channels)
//            .join();
//
//    return channelDrafts.stream()
//        .map(
//            channelDraft ->
//                ChannelDraftBuilder.of(channelDraft)
//                    .plusStores(ResourceIdentifier.ofKey(storeCologne.getKey()))
//                    .custom(
//                        CustomFieldsDraft.ofTypeKeyAndJson(
//                            "channel-type-gold", createCustomFieldsJsonMap()))
//                    .addresses(
//                        singletonList(
//                            Address.of(CountryCode.DE).withCity("cologne").withKey("address1")))
//                    .defaultBillingAddress(0)
//                    .billingAddresses(singletonList(0))
//                    .defaultShippingAddress(0)
//                    .shippingAddresses(singletonList(0))
//                    .build())
//        .collect(Collectors.toList());
//  }
}
