package com.ldtteam.teamcity.github.inspections;

public final class InspectionEntryData
{
    private final int lineNumber;
    private final String fqName;
    private final String result;
    private final int severity;

    public InspectionEntryData(final String[] data)
    {
        this.lineNumber = Integer.parseInt(data[0]);
        this.fqName = data[1];
        this.result = data[2];
        this.severity = Integer.parseInt(data[3]);
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public String getFqName()
    {
        return fqName;
    }

    public String getResult()
    {
        return result;
    }

    public int getSeverity()
    {
        return severity;
    }
}
