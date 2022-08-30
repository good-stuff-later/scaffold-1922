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
package net.lbruun.dbleaderelection.example2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import org.springframework.util.StringUtils;

/**
 *
 */
public class EventTypesListConversion  {

    private static final String ALL = "#ALL#";

    private EventTypesListConversion() {}
    
    public static EnumSet<LeaderElectorListener.EventType> convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String s = StringUtils.trimWhitespace(source);
        if (s.contains(ALL)) {
            if (s.equals(ALL)) {
                return EnumSet.allOf(LeaderElectorListener.EventType.class);
            } else {
                throw new IllegalArgumentException("Invalid value for EnumSet : " + source);
            }
        } else {
            s = StringUtils.trimAllWhitespace(s);
            String[] elements = s.split("\\,");
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
                EnumSet<LeaderElectorListener.EventType> plusses = EnumSet.copyOf(list);
                return EnumSet.complementOf(plusses);
            }
        }
    }
    
}
