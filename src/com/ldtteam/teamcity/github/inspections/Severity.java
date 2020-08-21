/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.ldtteam.teamcity.github.inspections;

import java.util.HashMap;
import java.util.Map;

public enum Severity
{
    ERROR(1, "Errors"),
    WARNING(2, "Warnings"),
    INFO(3, "Infos"),
    NOTE(4, "Notes"),
    NONE(5, "None");

    private final int level;
    private final String header;

    private static final Map<Integer, Severity> lookupMap = new HashMap<>();

    static  {
        for (final Severity severity : Severity.values())
        {
            lookupMap.put(severity.getLevel(), severity);
        }
    }

    Severity(final int level, final String header) {
        this.level = level;
        this.header = header;
    }

    public static Severity getFromLevel(final int level)
    {
        return lookupMap.get(level);
    }

    public int getLevel()
    {
        return level;
    }

    public String getHeader()
    {
        return header;
    }
}
