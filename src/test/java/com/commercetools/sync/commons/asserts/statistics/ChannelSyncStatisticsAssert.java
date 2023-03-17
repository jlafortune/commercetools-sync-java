package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.channels.helpers.ChannelSyncStatistics;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;

import javax.annotation.Nullable;

public final class ChannelSyncStatisticsAssert
    extends AbstractSyncStatisticsAssert<
        ChannelSyncStatisticsAssert, ChannelSyncStatistics> {

  ChannelSyncStatisticsAssert(@Nullable final ChannelSyncStatistics actual) {
    super(actual, ChannelSyncStatisticsAssert.class);
  }
}
