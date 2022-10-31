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
package org.keycloak.models.map.storage.mapper;

import org.keycloak.models.map.storage.mapper.MapperProviderFactory.FieldDescriptorGetter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.keycloak.models.map.storage.mapper.MapperProviderFactory.MapperFieldDescriptor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import org.jboss.logging.Logger;
import static org.keycloak.models.map.storage.mapper.MapperCastUtils.backCast;
import static org.keycloak.models.map.storage.mapper.MapperCastUtils.thereCast;

/**
 *
 * @author hmlnarik
 */
public class TemplateMapper {

    private static final Logger LOG = Logger.getLogger(TemplateMapper.class);

    private static final Pattern CONSTANT_TEMPLATE = Pattern.compile("((?:\\\\.|[^\\\\$])*?)");
    private static final Pattern DIRECT_EXPRESSION_TEMPLATE = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Pattern EXPRESSION_TEMPLATE = Pattern.compile("((?:\\\\.|[^\\\\$])*?)\\$\\{([^}]+)\\}");

    public static <R> Mapper<R> forTemplate(Object templateObj, FieldDescriptorGetter<R> innerFieldDescGetter, Class<?> targetFieldClass) {
        if (templateObj == null) {
            return EmptyMapper.instance();
        }

        // 1. if the template is not a String, it is treated as a constant
        if (! (templateObj instanceof String)) {
            return new ConstantMapper<>(templateObj, targetFieldClass);
        }

        String template = (String) templateObj;

        // 2. if the template is a String and has no substitutes in it, it is treated as a constant
        if (CONSTANT_TEMPLATE.matcher(template).matches()) {
            final String constant = template.replaceAll("\\\\(.)", "$1");
            return new ConstantMapper<>(constant, targetFieldClass);
        }

        // 3. If the template is of form ${XYZ}, then just map a field to another field
        //    TODO: Conversion may need to take place, e.g. when mapping an number to a String attribute and vice versa
        Matcher m = DIRECT_EXPRESSION_TEMPLATE.matcher(template);
        if (m.matches()) {
            final String originalExpr = m.group(1);
            return Optional.ofNullable(innerFieldDescGetter.getFieldDescriptor(originalExpr))
              .map(fd -> (Mapper<R>) new DirectMapper<R>(originalExpr, fd, targetFieldClass))
              .orElseGet(EmptyMapper::instance);
        }

        // 4. Now we know the pattern is more complex so it is treated as a String mapping
        m = EXPRESSION_TEMPLATE.matcher(template);
        LinkedList<Function<R, String>> thereEx = new LinkedList<>();
        List<BiConsumer<R, Object>> exprConsumerList = new LinkedList<>();
        List<BiConsumer<Map<String, Object>, Object>> exprConsumerMapList = new LinkedList<>();
        StringBuilder pat = new StringBuilder();
        int end = 0;

        // Parse regex and prepare for string concatenation
        while (m.find()) {
            final String originalExpr = m.group(2);

            String text = m.group(1).replaceAll("\\\\(.)", "$1");
            if (! text.isEmpty()) {
                thereEx.add(o -> text);
            }
            final Optional<MapperFieldDescriptor<R>> fd = Optional.ofNullable(innerFieldDescGetter.getFieldDescriptor(originalExpr));
            if (! fd.isPresent()) {
                throw new IllegalArgumentException("Unknown field name: " + originalExpr);
            }

            thereEx.add(fd.map(MapperFieldDescriptor::fieldGetter)
                .map(th -> thereCast(th, fd.get().getFieldClass(), targetFieldClass))
                .map(th -> (Function<R, String>) (o -> Objects.toString(th.apply(o), "")))
                .orElse(o -> "")
            );

            pat.append(Pattern.quote(text)).append("(.*?)");
            fd.map(MapperFieldDescriptor::fieldSetter)
              .map(setter -> backCast(setter, targetFieldClass, fd.get().getFieldClass()))
              .ifPresent(exprConsumerList::add);
            fd.map(MapperFieldDescriptor::fieldSetter)
              .map(setter -> backCast((Map<String, Object> map, Object o) -> map.put(originalExpr, o), targetFieldClass, fd.get().getFieldClass()))
              .ifPresent(exprConsumerMapList::add);
            end = m.end();
        }
        String endString = template.substring(end).replaceAll("\\\\(.)", "$1");
        if (! endString.isEmpty()) {
            thereEx.add(o -> endString);
        }
        pat.append(Pattern.quote(endString));

        Pattern p = Pattern.compile(pat.toString());
        BiConsumer[] exprConsumers = exprConsumerList.toArray(ALLOC_BICONSUMER);
        BiConsumer[] exprConsumersMap = exprConsumerMapList.toArray(ALLOC_BICONSUMER);

        return new BijectiveMapper<>(thereEx, p, exprConsumers, exprConsumersMap);
    }

    private static final IntFunction<BiConsumer[]> ALLOC_BICONSUMER = BiConsumer[]::new;

    private static class BijectiveMapper<T> implements Mapper<T> {

        private final LinkedList<Function<T, String>> thereEx;
        private final Pattern p;
        private final BiConsumer[] exprConsumers;
        private final BiConsumer[] exprConsumersMap;

        public BijectiveMapper(LinkedList<Function<T, String>> thereEx, Pattern p, BiConsumer[] exprConsumers, BiConsumer[] exprConsumersMap) {
            this.thereEx = thereEx;
            this.p = p;
            this.exprConsumers = exprConsumers;
            this.exprConsumersMap = exprConsumersMap;
        }

        @Override
        public Object there(T object) {
            StringBuilder sb = new StringBuilder();
            for (Function<T, String> f : thereEx) {
                sb.append(f.apply(object));
            }
            return sb.toString();
        }

        @Override
        public Class<?> getThereClass() {
            return String.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void back(T object, Object newValue) {
            if (newValue == null) {
                for (BiConsumer<T, String> exprConsumer : exprConsumers) {
                    exprConsumer.accept(object, null);
                }
                return;
            }
            Matcher matcher = p.matcher(newValue.toString());
            if (matcher.matches()) {
                for (int i = 0; i < matcher.groupCount(); i ++ ) {
                    @SuppressWarnings("unchecked")
                    final BiConsumer<T, String> exprConsumer = exprConsumers[i];
                    exprConsumer.accept(object, matcher.group(i + 1));
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> backToMap(Object newValue) {
            Map<String, Object> res = new HashMap<>();
            if (newValue == null) {
                for (BiConsumer<Map, String> exprConsumerMap : exprConsumersMap) {
                    exprConsumerMap.accept(res, null);
                }
                return res;
            }
            Matcher matcher = p.matcher(newValue.toString());
            if (matcher.matches()) {
                for (int i = 0; i < matcher.groupCount(); i ++ ) {
                    @SuppressWarnings("unchecked")
                    final BiConsumer<Map, String> exprConsumerMap = exprConsumersMap[i];
                    exprConsumerMap.accept(res, matcher.group(i + 1));
                }
            }

            return res;
        }
    }

}
