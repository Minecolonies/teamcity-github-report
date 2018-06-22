package com.ldtteam.teamcity.github.inspections;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.impl.codeInspection.InspectionInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class FileInspectionData
{
    private final String fileName;
    private final Map<Integer, LineInspectionData> lineInspectionDataMap;

    public FileInspectionData(final String fileName, final InspectionInfo inspectionInfo, final List<Long> inspectionIds)
    {
        this.fileName = fileName;
        this.lineInspectionDataMap = inspectionIds
          .stream()
          .flatMap(inspectionId -> inspectionInfo.getDetails(inspectionId, fileName, false).stream())
          .map(InspectionEntryData::new)
          .map(inspectionEntryData -> new HashMap.SimpleEntry<>(inspectionEntryData.getLineNumber(), inspectionEntryData))
          .collect(Collectors.toList())
          .stream()
          .collect(
            Collectors.toMap(
              entry -> entry.getKey(),
              entry -> new LineInspectionData(Lists.newArrayList(entry.getValue())),
              (entryOne, entryTwo) -> {
                r
              }
            )
          )
    }

}
