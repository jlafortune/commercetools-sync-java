package com.commercetools.sync.products;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.products.helpers.ProductSyncUtils.masterData;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;

public class ProductTestUtils {

    /**
     * Wraps provided string in {@link LocalizedString} of English {@link Locale}.
     */
    @Nullable
    public static LocalizedString en(@Nullable final String string) {
        return localizedString(string, ENGLISH);
    }

    /**
     * Provides the {@link ProductType} resource read from Json file.
     */
    public static ProductType productType() {
        return readObjectFromResource("product-type-main.json", ProductType.typeReference());
    }

    /**
     * Provides {@link ProductSyncOptions}.
     */
    @SuppressWarnings("unchecked")
    public static ProductSyncOptions syncOptions(final SphereClient client, final boolean publish,
                                                 final boolean updateStaged, final boolean revertStagedChanges) {
        BiConsumer errorCallBack = mock(BiConsumer.class);
        Consumer warningCallBack = mock(Consumer.class);
        return ProductSyncOptionsBuilder.of(client, errorCallBack, warningCallBack)
            .publish(publish)
            .revertStagedChanges(revertStagedChanges)
            .updateStaged(updateStaged)
            .build();
    }

    @SuppressWarnings("unchecked")
    public static ProductSyncOptions syncOptions(final boolean publish, final boolean updateStaged) {
        return syncOptions(mock(SphereClient.class), publish, updateStaged, false);
    }

    @SuppressWarnings("unchecked")
    public static ProductSyncOptions syncOptions(final boolean publish) {
        return syncOptions(mock(SphereClient.class), publish, true, false);
    }

    public static Product product(final String resourcePath) {
        return readObjectFromResource(resourcePath, Product.typeReference());
    }

    static <X> X join(final CompletionStage<X> stage) {
        return stage.toCompletableFuture().join();
    }

    @Nullable
    private static LocalizedString localizedString(final @Nullable String string, final Locale locale) {
        return isNull(string)
            ? null
            : LocalizedString.of(locale, string);
    }

    /**
     * Provides {@link ProductDraftBuilder} built on product template read from {@code resourcePath},
     * based on {@code productType} with {@code syncOptions} applied and category order hints set if {@code category}
     * is not null.
     */
    public static ProductDraftBuilder productDraftBuilder(final String resourcePath, final ProductType productType,
                                                          final ProductSyncOptions syncOptions) {
        Product template = product(resourcePath);
        ProductData productData = masterData(template, syncOptions);

        @SuppressWarnings("ConstantConditions")
        List<ProductVariantDraft> allVariants = productData.getAllVariants().stream()
            .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
            .collect(toList());

        return ProductDraftBuilder
            .of(productType, productData.getName(), productData.getSlug(), allVariants)
            .metaDescription(productData.getMetaDescription())
            .metaKeywords(productData.getMetaKeywords())
            .metaTitle(productData.getMetaTitle())
            .description(productData.getDescription())
            .searchKeywords(productData.getSearchKeywords())
            .key(template.getKey())
            .publish(template.getMasterData().isPublished())
            .categories(productData.getCategories())
            .categoryOrderHints(productData.getCategoryOrderHints());
    }
}
