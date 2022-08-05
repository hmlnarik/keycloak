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
package org.keycloak.models.map.storage.tree.mapper;

import org.keycloak.models.map.storage.mapper.Mapper;
import org.keycloak.models.map.storage.mapper.TemplateMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import org.keycloak.models.map.storage.mapper.MapperProviderFactory.MapperFieldDescriptor;

/**
 *
 * @author hmlnarik
 */
public class TemplateMapperTest {

    @Test
    public void testEmptyExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("", TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is(""));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(0));

        assertThat(m.backToMap(target).size(), is(0));
    }

    @Test
    public void testConstantIntExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("1", TemplateMapperTest::getTestFieldDescriptor, Integer.class);
        final Object target = m.there(null);

        assertThat(target, is(1));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(0));

        assertThat(m.backToMap(target).size(), is(0));
    }

    @Test
    public void testNullExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate(null, TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is(nullValue()));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(0));

        assertThat(m.backToMap(target).size(), is(0));
    }

    @Test
    public void testConstantExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("constant", TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is("constant"));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(0));

        assertThat(m.backToMap(target).size(), is(0));
    }

    @Test
    public void testVariableExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("{id}", TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is("ID"));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(1));
        assertThat(map, hasEntry("id", "ID"));

        final Map<String, Object> backToMap = m.backToMap(target);
        assertThat(backToMap.size(), is(1));
        assertThat(backToMap, hasEntry("id", "ID"));
    }

    @Test
    public void testIntegerToStringExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("{number/int}", TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is("12345"));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(1));
        assertThat(map, hasEntry("number/int", 12345));

        final Map<String, Object> backToMap = m.backToMap(target);
        assertThat(backToMap.size(), is(1));
        assertThat(backToMap, hasEntry("number/int", 12345));
    }

    @Test
    public void testLongToStringExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("{number/longString}", TemplateMapperTest::getTestFieldDescriptor, Long.class);
        final Object target = m.there(null);

        assertThat(target, is(67890L));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(1));
        assertThat(map, hasEntry("number/longString", "67890"));

        final Map<String, Object> backToMap = m.backToMap(target);
        assertThat(backToMap.size(), is(1));
        assertThat(backToMap, hasEntry("number/longString", "67890"));
    }

    @Test
    public void testIntegerToIntegerExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("{number/int}", TemplateMapperTest::getTestFieldDescriptor, Integer.class);
        final Object target = m.there(null);

        assertThat(target, is(12345));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(1));
        assertThat(map, hasEntry("number/int", 12345));

        final Map<String, Object> backToMap = m.backToMap(target);
        assertThat(backToMap.size(), is(1));
        assertThat(backToMap, hasEntry("number/int", 12345));
    }

    @Test
    public void testPrefixExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("prefix{id}", TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is("prefixID"));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(1));
        assertThat(map, hasEntry("id", "ID"));

        final Map<String, Object> backToMap = m.backToMap(target);
        assertThat(backToMap.size(), is(1));
        assertThat(backToMap, hasEntry("id", "ID"));
    }

    @Test
    public void testPrefixIntegerExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("prefix{number/int}", TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is("prefix12345"));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(1));
        assertThat(map, hasEntry("number/int", 12345));

        final Map<String, Object> backToMap = m.backToMap(target);
        assertThat(backToMap.size(), is(1));
        assertThat(backToMap, hasEntry("number/int", 12345));
    }

    @Test
    public void testSuffixExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("{id}suffix", TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is("IDsuffix"));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(1));
        assertThat(map, hasEntry("id", "ID"));

        final Map<String, Object> backToMap = m.backToMap(target);
        assertThat(backToMap.size(), is(1));
        assertThat(backToMap, hasEntry("id", "ID"));
    }

    @Test
    public void testComplexExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("a{id}b{field}c", TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is("aIDbFIELDc"));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(2));
        assertThat(map, hasEntry("id", "ID"));
        assertThat(map, hasEntry("field", "FIELD"));

        final Map<String, Object> backToMap = m.backToMap(target);
        assertThat(backToMap.size(), is(2));
        assertThat(backToMap, hasEntry("id", "ID"));
        assertThat(backToMap, hasEntry("field", "FIELD"));
    }

    @Test
    public void testComplexTypedExpression() {
        Mapper<Object> m = TemplateMapper.forTemplate("a{id}b{number/long}c", TemplateMapperTest::getTestFieldDescriptor, String.class);
        final Object target = m.there(null);

        assertThat(target, is("aIDb67890c"));
        HashMap<String, String> map = new HashMap<>();
        m.back(map, target);
        assertThat(map.size(), is(2));
        assertThat(map, hasEntry("id", "ID"));
        assertThat(map, hasEntry("number/long", 67890L));

        final Map<String, Object> backToMap = m.backToMap(target);
        assertThat(backToMap.size(), is(2));
        assertThat(backToMap, hasEntry("id", "ID"));
        assertThat(backToMap, hasEntry("number/long", 67890L));
    }

    public static MapperFieldDescriptor<Object> getTestFieldDescriptor(String fieldName) {
        return new MapperFieldDescriptor<Object>() {
            @Override
            public Function<Object, Object> fieldGetter() {
                return toUpperCaseFieldName(fieldName);
            }

            @Override
            public BiConsumer<Object, Object> fieldSetter() {
                return stringToMap(fieldName);
            }

            @SuppressWarnings("unchecked")
            private BiConsumer<Object, Object> stringToMap(String fieldName) {
                if (fieldName.equals("unset")) {
                    return null;
                }
                return (map, value) -> ((Map) map).put(fieldName, value);
            }

            private Function<Object, Object> toUpperCaseFieldName(String fieldName) {
                switch (fieldName) {
                    case "unset":
                        return null;
                    case "number/long":
                        return entity -> 67890L;
                    case "number/longString":
                        return entity -> "67890";
                    case "number/int":
                        return entity -> 12345;
                    default:
                        return entity -> fieldName.toUpperCase();
                }
            }

            @Override
            public Class<?> getFieldClass() {
                switch (fieldName) {
                    case "number/long":
                        return Long.class;
                    case "number/int":
                        return Integer.class;
                    default:
                        return String.class;
                }
            }
        };
    }
}
