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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PatchFileLineFinder
{
    private static final Pattern HUNKSTARTPATTERN = Pattern.compile("@@ -[0-9]+,[0-9]+ \\+(?<start>[0-9]+),(?<length>[0-9]+) @@");

    public static final int map(final List<String> input, final int searchedLineInTargetFile)
    {
        int firstHunkLine = -1;

        int targetHunkLine = -1;
        int targetHunkStart = -1;

        for (int i = 0; i < input.size(); i++)
        {
            final String line = input.get(i);

            final Matcher matcher = HUNKSTARTPATTERN.matcher(line);
            if (matcher.matches())
            {
                if (firstHunkLine == -1)
                    firstHunkLine = i;

                final Integer start = Integer.parseInt(matcher.group("start"));
                final Integer length = Integer.parseInt(matcher.group("length"));

                if (start > searchedLineInTargetFile)
                    break;

                if ((start + length) >= searchedLineInTargetFile)
                {
                    targetHunkLine = i;
                    targetHunkStart = start;
                    break;
                }
            }
        }

        if (targetHunkLine == -1)
            return -1;

        int resultingFileLine = targetHunkStart - 1;
        for (int i = targetHunkLine + 1; i < input.size(); i++)
        {
            final String line = input.get(i);
            if (!(line.startsWith("- ") || line.startsWith("--")))
                resultingFileLine ++;

            if (resultingFileLine == searchedLineInTargetFile)
            {
                return i - firstHunkLine;
            }
        }

        return -1;
    }
}
