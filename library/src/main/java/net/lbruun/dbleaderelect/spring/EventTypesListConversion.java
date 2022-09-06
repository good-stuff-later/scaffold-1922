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
package net.lbruun.dbleaderelect.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import net.lbruun.dbleaderelect.LeaderElectorListener;

/**
 * Converts from a string list (comma-separated) into an
 * {@code EnumSet<LeaderElectorListener.EventType>}.
 * 
 * <p>
 * The string list must be either:
 * 
 * <ul>
 *   <li>The text {@code "#ALL#"}. This means all possible values of the Enum.</li>
 *   <li>Comma-separated list of enums to be excluded. Each enum must be prefixed
 *       with a {@code "-"}. For example, the value {@code "-X1,-X2"} means 
 *       all enums <i>except</i> {@code X1} and {@code X2}.</li>
 *   <li>Comma-separated list of enums to be included. For example, the value {@code "X1,X2"} 
 *       means <i>only</i> the enums {@code X1} and {@code X2}.</li>
 * </ul>
 * 
 * <p>
 * The difference from Spring Boot's own implementation is the ability to
 * specify 'all' and the ability to specify 'all, except..'.
 */
class EventTypesListConversion  {

    private static final String ALL = "#ALL#";

    private EventTypesListConversion() {}
    
    public static EnumSet<LeaderElectorListener.EventType> convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        
        String s = source.trim();
        if (s.contains(ALL)) {
            if (s.equals(ALL)) {
                return EnumSet.allOf(LeaderElectorListener.EventType.class);
            } else {
                throw new IllegalArgumentException("Invalid value for EnumSet : " + source);
            }
        } else {
            String[] elements = s.split("\\s*\\,\\s*");
            if (s.contains("-")) {
                if (!Arrays.stream(elements).allMatch((t) -> {
                    return t.startsWith("-") && (t.length() > 1);
                })) {
                    throw new IllegalArgumentException("Invalid value for EnumSet : \"" + source + "\". Either all values in list must start with '-' or none.");
                }
                List<LeaderElectorListener.EventType> list = new ArrayList<>();
                for (String e : elements) {
                    list.add(LeaderElectorListener.EventType.valueOf(e.substring(1)));
                }
                EnumSet<LeaderElectorListener.EventType> minuses = EnumSet.copyOf(list);
                return EnumSet.complementOf(minuses);
            } else {
                List<LeaderElectorListener.EventType> list = new ArrayList<>();
                for (String e : elements) {
                    list.add(LeaderElectorListener.EventType.valueOf(e));
                }
                return EnumSet.copyOf(list);
            }
        }
    }
}
