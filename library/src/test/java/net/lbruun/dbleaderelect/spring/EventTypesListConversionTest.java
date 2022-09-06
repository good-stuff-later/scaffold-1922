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

import java.util.EnumSet;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.LeaderElectorListener.EventType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EventTypesListConversionTest {

    @Test
    public void testConvertAll() {
        System.out.println("convert-ALL");
        String source = "#ALL#";
        EnumSet<LeaderElectorListener.EventType> expResult = EnumSet.allOf(LeaderElectorListener.EventType.class);
        EnumSet<LeaderElectorListener.EventType> result = EventTypesListConversion.convert(source);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvertExcept() {
        System.out.println("convert-MINUSSES");
        String source = "-LEADERSHIP_LOST,-LEADERSHIP_ASSUMED";
        EnumSet<EventType> minusses = EnumSet.of(EventType.LEADERSHIP_LOST, EventType.LEADERSHIP_ASSUMED);
        EnumSet<EventType> expResult = EnumSet.complementOf(minusses);
        EnumSet<EventType> result = EventTypesListConversion.convert(source);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvertInclude() {
        System.out.println("convert-PLUSSES");
        String source = "LEADERSHIP_LOST,LEADERSHIP_ASSUMED";
        EnumSet<EventType> expResult = EnumSet.of(EventType.LEADERSHIP_LOST, EventType.LEADERSHIP_ASSUMED);
        EnumSet<EventType> result = EventTypesListConversion.convert(source);
        assertEquals(expResult, result);
    }
}
