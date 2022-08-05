/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.map.storage.tree;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.StringKeyConverter.StringKey;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.mapper.MappersMap;
import org.keycloak.storage.SearchableModelField;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Stream that has a post-processing filter attached.
 * <p>
 * <b>WARNING:</b> This is a pragmatic class which may lead to type-unsafe operations.
 *                 Use only when you know exactly what you are doing.
 *
 * @author hmlnarik
 */
public class StreamWithPostFiltering<V> implements Stream<V> {

    private final Stream<V> delegate;

    private QueryParameters<?> remainingQueryParameters;

    private final Class<? extends AbstractEntity> entityClass;

    public static <V extends AbstractEntity> Stream<V> with(Stream<V> delegate, QueryParameters<?> remainingQuery, Class<? extends AbstractEntity> entityClass) {
        return new StreamWithPostFiltering<>(delegate, remainingQuery, entityClass);
    }

    private StreamWithPostFiltering(Stream<V> delegate, QueryParameters<?> remainingQuery, Class<? extends AbstractEntity> entityClass) {
        this.delegate = delegate;
        this.remainingQueryParameters = remainingQuery;
        this.entityClass = entityClass;
    }

    public <M> Stream<V> applyPostFilter() {
        final DefaultModelCriteria<?> mcb = remainingQueryParameters.getModelCriteriaBuilder();
        if (mcb.isAlwaysTrue()) {
            return delegate;
        }
        if (mcb.isAlwaysFalse()) {
            return Stream.empty();
        }
        final Class<M> modelClass = ModelEntityUtil.getModelType(entityClass);
        final Map<SearchableModelField<? super M>, UpdatePredicatesFunc<String, AbstractEntity, M>> predicates = MapFieldPredicates.getPredicates(modelClass);
        return delegate.filter(mcb.flashToModelCriteriaBuilder(new MapModelCriteriaBuilder(StringKey.INSTANCE, predicates)));
    }

    public <V1 extends AbstractEntity> void reevaluatePostFilter(MappersMap<V1, V1> mappers) {
        if (mappers != null && ! mappers.keySet().isEmpty()) {
            remainingQueryParameters = remainingQueryParameters.transform(mappers::evaluate);
        }
    }

    @Override
    public StreamWithPostFiltering<V> filter(Predicate<? super V> predicate) {
        return new StreamWithPostFiltering<>(delegate.filter(predicate), remainingQueryParameters, entityClass);
    }

    @Override
    public <R> StreamWithPostFiltering<R> map(Function<? super V, ? extends R> mapper) {
        return new StreamWithPostFiltering<>(delegate.map(mapper), remainingQueryParameters, entityClass);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super V> mapper) {
        return applyPostFilter().mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super V> mapper) {
        return applyPostFilter().mapToLong(mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super V> mapper) {
        return applyPostFilter().mapToDouble(mapper);
    }

    @Override
    public <R> StreamWithPostFiltering<R> flatMap(Function<? super V, ? extends Stream<? extends R>> mapper) {
        return new StreamWithPostFiltering<>(delegate.flatMap(mapper), remainingQueryParameters, entityClass);
    }

    @Override
    public IntStream flatMapToInt(Function<? super V, ? extends IntStream> mapper) {
        return applyPostFilter().flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super V, ? extends LongStream> mapper) {
        return applyPostFilter().flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super V, ? extends DoubleStream> mapper) {
        return applyPostFilter().flatMapToDouble(mapper);
    }

    @Override
    public StreamWithPostFiltering<V> distinct() {
        return new StreamWithPostFiltering<>(delegate.distinct(), remainingQueryParameters, entityClass);
    }

    @Override
    public StreamWithPostFiltering<V> sorted() {
        return new StreamWithPostFiltering<>(delegate.sorted(), remainingQueryParameters, entityClass);
    }

    @Override
    public StreamWithPostFiltering<V> sorted(Comparator<? super V> comparator) {
        return new StreamWithPostFiltering<>(delegate.sorted(comparator), remainingQueryParameters, entityClass);
    }

    @Override
    public StreamWithPostFiltering<V> peek(Consumer<? super V> action) {
        return new StreamWithPostFiltering<>(delegate.peek(action), remainingQueryParameters, entityClass);
    }

    @Override
    public StreamWithPostFiltering<V> limit(long maxSize) {
        return new StreamWithPostFiltering<>(delegate.limit(maxSize), remainingQueryParameters, entityClass);
    }

    @Override
    public StreamWithPostFiltering<V> skip(long n) {
        return new StreamWithPostFiltering<>(delegate.skip(n), remainingQueryParameters, entityClass);
    }

    @Override
    public void forEach(Consumer<? super V> action) {
        applyPostFilter().forEach(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super V> action) {
        applyPostFilter().forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return applyPostFilter().toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return applyPostFilter().toArray(generator);
    }

    @Override
    public V reduce(V identity, BinaryOperator<V> accumulator) {
        return applyPostFilter().reduce(identity, accumulator);
    }

    @Override
    public Optional<V> reduce(BinaryOperator<V> accumulator) {
        return applyPostFilter().reduce(accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super V, U> accumulator, BinaryOperator<U> combiner) {
        return applyPostFilter().reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super V> accumulator, BiConsumer<R, R> combiner) {
        return applyPostFilter().collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(Collector<? super V, A, R> collector) {
        return applyPostFilter().collect(collector);
    }

    @Override
    public Optional<V> min(Comparator<? super V> comparator) {
        return applyPostFilter().min(comparator);
    }

    @Override
    public Optional<V> max(Comparator<? super V> comparator) {
        return applyPostFilter().max(comparator);
    }

    @Override
    public long count() {
        return applyPostFilter().count();
    }

    @Override
    public boolean anyMatch(Predicate<? super V> predicate) {
        return applyPostFilter().anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super V> predicate) {
        return applyPostFilter().allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super V> predicate) {
        return applyPostFilter().noneMatch(predicate);
    }

    @Override
    public Optional<V> findFirst() {
        return applyPostFilter().findFirst();
    }

    @Override
    public Optional<V> findAny() {
        return applyPostFilter().findAny();
    }

    @Override
    public Iterator<V> iterator() {
        return applyPostFilter().iterator();
    }

    @Override
    public Spliterator<V> spliterator() {
        return applyPostFilter().spliterator();
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    @Override
    public StreamWithPostFiltering<V> sequential() {
        return new StreamWithPostFiltering<>(delegate.sequential(), remainingQueryParameters, entityClass);
    }

    @Override
    public StreamWithPostFiltering<V> parallel() {
        return new StreamWithPostFiltering<>(delegate.parallel(), remainingQueryParameters, entityClass);
    }

    @Override
    public StreamWithPostFiltering<V> unordered() {
        return new StreamWithPostFiltering<>(delegate.unordered(), remainingQueryParameters, entityClass);
    }

    @Override
    public StreamWithPostFiltering<V> onClose(Runnable closeHandler) {
        return new StreamWithPostFiltering<>(delegate.onClose(closeHandler), remainingQueryParameters, entityClass);
    }

    @Override
    public void close() {
        delegate.close();
    }

}
