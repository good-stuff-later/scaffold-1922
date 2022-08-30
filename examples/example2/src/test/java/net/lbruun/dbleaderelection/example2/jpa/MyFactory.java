/*
 * Copyright 2022 lbruun.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lbruun.dbleaderelection.example2.jpa;

import java.util.EnumSet;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

public class MyFactory implements ConverterFactory<String, EnumSet> {

    @Override
    public <T extends EnumSet> Converter<String, T> getConverter(Class<T> targetType) {
        Class<?>[] resolveTypeArguments = GenericTypeResolver.resolveTypeArguments(targetType, Enum.class);
        return null;
    }


}
