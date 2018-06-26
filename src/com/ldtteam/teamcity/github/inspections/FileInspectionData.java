package com.ldtteam.teamcity.github.inspections;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.impl.codeInspection.InspectionInfo;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHPullRequestReviewBuilder;

import java.util.*;
import java.util.stream.Collectors;

public final class FileInspectionData
{
    private final String                           fileName;
    private final Map<Integer, LineInspectionData> lineInspectionDataMap;

    public FileInspectionData(final String fileName, final Collection<InspectionInfo> inspectionInfos, final Collection<Long> inspectionIds, final GHPullRequest pullRequest)
    {
        this.fileName = fileName;
        this.lineInspectionDataMap = generateRemappedAndUpdateLineMappings(
          fileName,
          inspectionIds
            .stream()
            .flatMap(inspectionId -> inspectionInfos
              .stream()
              .flatMap(
                inspectionInfo -> inspectionInfo.getDetails(inspectionId, fileName, false).stream()
              ))
            .distinct()
            .map(InspectionEntryData::new)
            .map(inspectionEntryData -> new HashMap.SimpleEntry<>(inspectionEntryData.getLineNumber(),
              inspectionEntryData))
            .collect(Collectors.toList())
            .stream()
            .collect(
              Collectors.toMap(
                AbstractMap.SimpleEntry::getKey,
                entry -> new LineInspectionData(Lists.newArrayList(entry.getValue())),
                (entryOne, entryTwo) -> {
                    final List<InspectionEntryData> combinedRaw = new ArrayList<>();
                    combinedRaw.addAll(entryOne.getRaw());
                    combinedRaw.addAll(entryTwo.getRaw());

                    return new LineInspectionData(combinedRaw);
                }
              )
            ),
          pullRequest
        );
    }

    private Map<Integer, LineInspectionData> generateRemappedAndUpdateLineMappings(
      final String fileName,
      final Map<Integer, LineInspectionData> lineInspectionDataMap,
      final GHPullRequest pullRequest)
    {
        final List<GHPullRequestFileDetail> fileDetails = pullRequest.listFiles().asList();
        final GHPullRequestFileDetail targetDetail = fileDetails.stream().filter(d -> d.getFilename().equals(fileName)).findFirst().orElseThrow(() -> new IllegalStateException("Can not find change on Github."));
        final List<String> patchContents = Arrays.asList(targetDetail.getPatch().split("\n"));

        final Map<Integer, LineInspectionData> remappedData = new HashMap<>();
        for (Map.Entry<Integer, LineInspectionData> dataEntry :
          lineInspectionDataMap.entrySet())
        {
            final int remapped = getRemappedLineInspectionData(dataEntry.getValue(), patchContents);
            if (remapped != -1)
            {
                remappedData.put(remapped, dataEntry.getValue());
            }
        }

        return remappedData;
    }

    private final int getRemappedLineInspectionData(final LineInspectionData lineInspectionData, final List<String> patchContents)
    {
        return PatchFileLineFinder.map(patchContents, lineInspectionData.getLine());
    }

    public String getFileName()
    {
        return fileName;
    }

    public Map<Integer, LineInspectionData> getLineInspectionDataMap()
    {
        return lineInspectionDataMap;
    }

    public void process(final GHPullRequestReviewBuilder builder)
    {
        this.lineInspectionDataMap.keySet().forEach(line -> {
            final LineInspectionData data = lineInspectionDataMap.get(line);
            builder.comment(data.toBody(), fileName, line);
        });
    }
}
