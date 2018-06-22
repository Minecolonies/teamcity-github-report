package com.ldtteam.teamcity.github.inspections;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.steppschuh.markdowngenerator.list.UnorderedList;
import net.steppschuh.markdowngenerator.text.heading.Heading;

import java.util.*;
import java.util.stream.Collectors;

public final class LineInspectionData
{
    private final int                            line;
    private final Collection<InspectionEntryData> raw;
    private final EnumMap<Severity, Set<String>> resultsForSeverity;

    public LineInspectionData(final Collection<InspectionEntryData> entryData)
    {
        if (entryData.stream().map(InspectionEntryData::getLineNumber).distinct().count() != 1)
            throw new IllegalArgumentException("Entry data contains contents for not exactly one line.");

        this.line = entryData.stream().map(InspectionEntryData::getLineNumber).findFirst().orElseThrow(() ->new IllegalStateException("Argument check failed"));
        this.raw = entryData;
        this.resultsForSeverity = new EnumMap<>(entryData
                                                  .stream()
                                                  .collect(
                                                    Collectors.toMap(
                                                      entry -> Severity.getFromLevel(entry.getSeverity()),
                                                      entry -> ImmutableSet.of(entry.getResult()),
                                                      (iSetOne, iSetTwo) -> {
                                                          final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                                                          builder.addAll(iSetOne);
                                                          builder.addAll(iSetTwo);
                                                          return builder.build();
                                                      }
                                                    )));
    }

    public int getLine()
    {
        return line;
    }

    public EnumMap<Severity, Set<String>> getResultsForSeverity()
    {
        return resultsForSeverity;
    }

    public Collection<InspectionEntryData> getRaw()
    {
        return raw;
    }

    @Override
    public String toString()
    {
        return "LineInspectionData{" +
                 "line=" + line +
                 ", resultsForSeverity=" + resultsForSeverity +
                 '}';
    }

    public String toBody()
    {
        StringBuilder builder = new StringBuilder();

        for (Severity severity : this.resultsForSeverity.keySet())
        {
            if (builder.toString() != "")
                builder = builder.append("\n");

            final Set<String> results = resultsForSeverity.get(severity);
            builder = builder.append(new Heading(severity.getHeader() +  ":", 3)).append("\n");
            builder = builder.append(new UnorderedList<>(ImmutableList.copyOf(results)));
        }

        return builder.toString();
    }
}
